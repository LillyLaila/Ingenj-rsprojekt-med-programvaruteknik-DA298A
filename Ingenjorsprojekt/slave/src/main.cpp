#include <Arduino.h>
#include <WiFi.h>
#include <esp_now.h>
#include <SPI.h>
#include <TFT_eSPI.h>

// TFT setup
TFT_eSPI tft = TFT_eSPI();
uint8_t gatewayAddress[] = {0xD4, 0x8A, 0xFC, 0xA2, 0x9D, 0x18}; // ändra ifall du använder en annan gateway.
uint8_t mac[6];

// Poisitonering
int currentX = 0;
int currentY = 0;
int targetX = -1;
int targetY = -1;
int previousX = -1;
int previousY = -1;

// Define map dimensions
#define MAP_WIDTH 8
#define MAP_HEIGHT 6
#define CELL_SIZE 2
// Store current map state for visualization
char mapState[MAP_HEIGHT * CELL_SIZE][MAP_WIDTH * CELL_SIZE];
const int cellSize = 18; // 18 är perfekt för hela displayen
const int startX = 10;   // Kartan ska börja här i tft displayen
const int startY = 10;
// Kollar om start meddelande har skickats
bool gameStarted = false;
//kollar om meddelandet har skickats till server.


// States
enum State
{
  SEARCH_BUILDING,
  MISSION_REQUEST,
  LOCATE_FIRE,
  WAITING_AT_SCENE,
  READY // Vänta på instruktioner (väntande tillstånd)
};
State currentState = SEARCH_BUILDING;

bool helpMessageSent = false;
bool sufficientNodesAtLocation = false;
bool targetReached = false;

// Function to draw a grid cell
void drawCell(int x, int y, char type)
{
  switch (type)
  {
  case '-': // Tom ruta, använda mörk grå för att markera skillnad
    tft.fillRect(x, y, cellSize, cellSize, TFT_DARKGREY);
    break;
  case '#': // Vägg
    tft.fillRect(x, y, cellSize, cellSize, TFT_BLUE);
    tft.drawLine(x, y, x + cellSize, y + cellSize, TFT_WHITE); // Diagonal
    tft.drawLine(x + cellSize, y, x, y + cellSize, TFT_WHITE); // Diagonal
    break;
  case 'F': // Eld
    tft.fillRect(x, y, cellSize, cellSize, TFT_RED);
    tft.fillCircle(x + cellSize / 2, y + cellSize / 2, cellSize / 4, TFT_YELLOW);
    break;
  case 'B': // Brandman
    tft.fillRect(x, y, cellSize, cellSize, TFT_CYAN);
    tft.drawCircle(x + cellSize / 2, y + cellSize / 2, cellSize / 4, TFT_WHITE);
    break;
  case 'R': // Rök
    tft.fillRect(x, y, cellSize, cellSize, TFT_DARKGREY);
    tft.fillCircle(x + cellSize / 3, y + cellSize / 3, cellSize / 4, TFT_LIGHTGREY);
    tft.fillCircle(x + 2 * cellSize / 3, y + 2 * cellSize / 3, cellSize / 4, TFT_WHITE);
    break;
  case 'S': // Skadad
    tft.fillRect(x, y, cellSize, cellSize, TFT_GREEN);
    break;
  case 'X': // Brännbart
    tft.fillRect(x, y, cellSize, cellSize, TFT_ORANGE);
    break;
  default: // Okänt
    tft.fillRect(x, y, cellSize, cellSize, TFT_PURPLE);
    break;
  }
  // Rita kantlinjer
  tft.drawRect(x, y, cellSize, cellSize, TFT_LIGHTGREY);
}

// Function to initialize map grid
void drawGrid()
{
  for (int regionY = 0; regionY < MAP_HEIGHT; ++regionY)
  {
    for (int regionX = 0; regionX < MAP_WIDTH; ++regionX)
    {
      for (int cellY = 0; cellY < CELL_SIZE; ++cellY)
      {
        for (int cellX = 0; cellX < CELL_SIZE; ++cellX)
        {
          int pixelX = startX + (regionX * CELL_SIZE + cellX) * cellSize;
          int pixelY = startY + (regionY * CELL_SIZE + cellY) * cellSize;
          mapState[regionY * CELL_SIZE + cellY][regionX * CELL_SIZE + cellX] = '-';
          drawCell(pixelX, pixelY, '-'); // Initiera alla celler till tomma vid starten
        }
      }
    }
  }
}
// Update specific cell in the grid
void updateCell(int regionX, int regionY, int cellX, int cellY, char type)
{
  int indexX = regionX * CELL_SIZE + cellX; //behövs ändringar???  Felsök här....
  int indexY = regionY * CELL_SIZE + cellY;

  if (mapState[indexY][indexX] != type)
  {
    // Update only if the state has changed
    int pixelX = startX + indexX * cellSize;
    int pixelY = startY + indexY * cellSize;
    drawCell(pixelX, pixelY, type);
    mapState[indexY][indexX] = type; // Update the stored state
  }
}

// Hanterar data från gateaway (info om brand,rök) för att uppdatera kartan
void onDataReceive(const uint8_t *macAddr, const uint8_t *data, int len)
{
  char incomingMessage[len + 1];
  memcpy(incomingMessage, data, len);
  incomingMessage[len] = '\0';

  if (strcmp(incomingMessage, "START:OK") == 0)
  {
    gameStarted = true;
    Serial.println("Game started signal received.");
  }


  // Kollar vilken state en brandman har,
  // State = waiting och inte tillräckligt med förstärkning så return.
  if (currentState == WAITING_AT_SCENE && !sufficientNodesAtLocation)
  {
    Serial.println("WAIT: Avböjer nytt mål eftersom förstärkning saknas");
    return;
  }

  // TEST, för att se ifall något hämtas från gateway.
   Serial.println(" received from server:");
  Serial.println(incomingMessage);

  // Expected message format: "TYPE:x,y,cX,CY" where TYPE is 'F' or 'R' or 'B'
  char type;
  int x, y, cellX, cellY;
  if (sscanf(incomingMessage, "%c,%d,%d,%d,%d", &type, &x, &y, &cellX, &cellY) == 5)
  {
    updateCell(x, y, cellX, cellY, type);
  }
}

// Tar bort den tidigare positionen
void clearPreviousPosition()
{
  if (previousX != -1 && previousY != -1)
  {
    // Ta bort tidigare brandmansmarkering
    int regionX = previousX;
    int regionY = previousY;
    int cellX = previousX % CELL_SIZE;
    int cellY = previousY % CELL_SIZE;
    updateCell(regionX, regionY, cellX, cellY, '-'); // Återställ cellen till tom
  }
}

// Skickar koordinater på brandman till gateway
void sendCoordinatesToGateway()
{

  if (previousX == currentX && previousY == currentY)
  {
    return;
  }
    clearPreviousPosition();

  previousX = currentX;
  previousY = currentY;

  int regionX = currentX;
  int regionY = currentY;
  int cellX = currentX % CELL_SIZE;
  int cellY = currentY % CELL_SIZE;

  // Skickar UPDATE:(x,y)(cx,cy) till gateaway  (koordinat till brandman)
  char message[64];
  snprintf(message, sizeof(message), "UPDATE:%d,%d,%d,%d,%02X:%02X:%02X:%02X:%02X:%02X",
           regionX, regionY, cellX, cellY, mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
  esp_err_t result = esp_now_send(gatewayAddress, (uint8_t *)message, strlen(message));

  if (result == ESP_OK)
  {
    Serial.printf("Position skickad: Region (%d, %d), Cell (%d, %d), MAC: %02X:%02X:%02X:%02X:%02X:%02X \n", regionX, regionY, cellX, cellY, mac[0], mac[1], mac[2], mac[3], mac[4], mac[5]);
    updateCell(regionX, regionY, cellX, cellY, 'B');
  }
  else
  {
    Serial.println("Misslyckades med att skicka position.");
  }
}
void sendMessage(const char *message)
{
   esp_err_t result = esp_now_send(gatewayAddress, (uint8_t *)message, strlen(message));
    if (result == ESP_OK) {
        Serial.printf("Sending message: %s\n", message);
    } else {
        Serial.println("Failed to send message.");
    }
}

void searchBuilding()
{
  delay(5000);
  int direction = random(0, 2);
  if (direction == 0)
  {
    if (random(0, 2) == 0)
    {
      currentX++;
    }
    else
    {
      currentX--;
    }
  }
  else
  {
    if (random(0, 2) == 0)
    {
      currentY++;
    }
    else
    {
      currentY--;
    }
  }
  if (currentX < 0)
    currentX = 0;
  if (currentX > 7)
    currentX = 7;

  if (currentY < 0)
    currentY = 0;
  if (currentY > 5)
    currentY = 5;

  Serial.printf("SEARCH_BUILDING: X=%d, Y=%d\n", currentX, currentY);
}

void moveTowardsTarget()
{
  if (currentX != targetX)
  {
    currentX += (currentX < targetX) ? 1 : -1;
  }
  else if (currentY != targetY)
  {
    currentY += (currentY < targetY) ? 1 : -1;
  }

  Serial.printf("LOCATE_FIRE: Rör sig mot mål: X = %d, Y=%d (mål: X=%d, Y=%d)", currentX, currentY, targetX, targetY);

  if (currentX == targetX && currentY == targetY)
  {
    Serial.println("LOCATE_FIRE: Mål nått!");
    targetReached = true;
    currentState = WAITING_AT_SCENE;
  }
}

void waitForReinforcements()
{
  Serial.println("WAIT: Väntar på fler noder...");
  if (sufficientNodesAtLocation)
  {
    Serial.println("WAIT: Tillräckligt med noder på plats! Släcker brand...");
    currentState = SEARCH_BUILDING;
    targetX = -1;
    targetY = -1;
    targetReached = false;
  }
}

void setup()
{
  Serial.begin(115200);
  WiFi.mode(WIFI_STA);
  WiFi.macAddress(mac);

  // TFT initialization
  tft.init();
  Serial.println("TFT initierad!");
  tft.setRotation(1);
  tft.fillScreen(TFT_BLACK);

  drawGrid(); // Draw the initial grid

  // ESP-NOW initialization
  if (esp_now_init() != ESP_OK)
  {
    Serial.println("ESP-NOW initialization failed");
    return;
  }
  esp_now_register_recv_cb(onDataReceive);
  esp_now_peer_info_t peerInfo;
  memset(&peerInfo, 0, sizeof(peerInfo));
  memcpy(peerInfo.peer_addr, gatewayAddress, 6);
  peerInfo.channel = 0;
  peerInfo.encrypt = false;
  if (esp_now_add_peer(&peerInfo) != ESP_OK)
  {
    Serial.println("Failed to add peer");
    return;
  }
      sendMessage("READY");

}

void loop()
{
  if (!gameStarted)
  {
    return;
  }
  sendCoordinatesToGateway();

  switch (currentState)
  {
  case SEARCH_BUILDING:
    searchBuilding();
    break;
  case MISSION_REQUEST:
    Serial.println("MISSION_REQUEST: Uppdrag mottaget, byter till LOCATE_FIRE");
    currentState = LOCATE_FIRE;
    break;
  case WAITING_AT_SCENE:
    waitForReinforcements();
    break;
  }

  delay(1000);
}