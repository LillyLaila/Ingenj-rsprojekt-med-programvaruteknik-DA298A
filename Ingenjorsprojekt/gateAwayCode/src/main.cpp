#include <WiFi.h>
#include <esp_now.h>
#include <map>
#include <vector>

uint8_t broadcastAddress[] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
const int ledPin = 2;
int targetX = 5;
int targetY = 7;

bool gameStarted = false;


// Track ready status of slaves by MAC address
std::map<std::string, bool> slaveReadyStatus;


void handleIncomingMessage(const uint8_t *macAddr, const uint8_t *data, int len);
void sendTargetToAllNodes(char type, int regionX, int regionY, int cellX, int cellY);
void sendToAllNodes(const char *message);



// Initiera ESPNOW
void initESPNow()
{
    if (esp_now_init() == ESP_OK)
    {
        Serial.println("ESP-NOW initialized successfully");
    }
    else
    {
        Serial.println("Error initializing ESP-NOW");
        ESP.restart();
    }

    esp_now_peer_info_t peerInfo{};
    memset(&peerInfo, 0, sizeof(peerInfo));
    memcpy(peerInfo.peer_addr, broadcastAddress, 6);
    peerInfo.channel = 0;
    peerInfo.encrypt = false;
    peerInfo.ifidx = WIFI_IF_STA;

    if (esp_now_add_peer(&peerInfo) != ESP_OK)
    {
        Serial.println("Failed to add peer");
        ESP.restart();
    }

    esp_now_register_recv_cb(handleIncomingMessage);
}

void onSlaveReady(const uint8_t *macAddr) {
    char macStr[18];
    snprintf(macStr, sizeof(macStr), "%02X:%02X:%02X:%02X:%02X:%02X", 
             macAddr[0], macAddr[1], macAddr[2], macAddr[3], macAddr[4], macAddr[5]);

    // Register the slave as ready
    slaveReadyStatus[macStr] = true;

    // Check if all slaves are ready
    bool allReady = true;
    for (const auto& status : slaveReadyStatus) {
        if (!status.second) {
            allReady = false;
            break;
        }
    }
        // If all slaves are ready, broadcast start message
    if (allReady) {
        sendToAllNodes("START:OK");
    }
}

// Skickar en update om nodems position till servern
// Skickar en update om nodens position till servern
void sendToServer(const char *message) {
    
        String formattedMessage = String("UPDATE:") + message + "\n";
        Serial.printf("Sending to server: %s\n", formattedMessage.c_str());
}

// Handle incoming messages
void handleIncomingMessage(const uint8_t *macAddr, const uint8_t *data, int len)
{
    char message[32];
    snprintf(message, sizeof(message), "%.*s", len, data);
    int regionX, regionY, cellX, cellY;
    char type;
    char mac[18];

    // Kollar om man får ett start meddelande från servern.
    if (strcmp(message, "START:OK") == 0)
    {
        sendToAllNodes("START");
    gameStarted = true;

        return;
    }
        if (strcmp(message, "READY") == 0) {
        Serial.printf("Received READY from %02X:%02X:%02X:%02X:%02X:%02X\n",
                      macAddr[0], macAddr[1], macAddr[2], macAddr[3], macAddr[4], macAddr[5]);
        onSlaveReady(macAddr);
    }


    // Ta emot meddelanden från noden och formattera
    if (sscanf(message + 7, "%d,%d,%d,%d,%17s", &regionX, &regionY, &cellX, &cellY, mac) == 5)
    {
        char formattedMessage[64];
        snprintf(formattedMessage, sizeof(formattedMessage), "UPDATE:%d,%d,%d,%d,%s", regionX, regionY, cellX, cellY, mac);
        Serial.println(formattedMessage);
        
    }

    // Tar emot meddelanden från servern med denna format och skickar till alla noder .
    if (sscanf(message, "%c:%d,%d,%d,%d", &type, &regionX, &regionY, &cellX, &cellY) == 5)
    {
        // Skickar till alla noder
        sendTargetToAllNodes(type, regionX, regionY, cellX, cellY);
    }
}

// Skicka START meddelande till alla noder.
void sendToAllNodes(const char *message)
{
    esp_err_t result = esp_now_send(broadcastAddress, (uint8_t *)message, strlen(message));
    if (result == ESP_OK)
    {
        Serial.printf("Broadcast skickat: %s\n", message);
    }
    else
    {
        Serial.println("Misslyckades med att skicka broadcast.");
    }
}

// Skickar data till alla noder
void sendTargetToAllNodes(char type, int regionX, int regionY, int cellX, int cellY)
{
    char message[32];
    snprintf(message, sizeof(message), "%c,%d,%d,%d,%d", type, regionX, regionY, cellX, cellY);

    esp_err_t result = esp_now_send(broadcastAddress, (uint8_t *)message, strlen(message));
    if (result == ESP_OK)
    {
        Serial.printf("Broadcast skickat: %s\n", message);
    }
    else
    {
        Serial.println("Misslyckades med att skicka broadcast.");
    }
}

void setup()
{
    Serial.begin(115200);
    WiFi.mode(WIFI_STA);
    Serial.print("MAC Address: ");
    Serial.println(WiFi.macAddress());

    initESPNow();
}

void loop()
{
    delay(100);

    if (Serial.available() > 0)
    {
        String receivedMessage = Serial.readStringUntil('\n'); // Läser från seriella porten

        char message[32];
        receivedMessage.toCharArray(message, sizeof(message));

        handleIncomingMessage(nullptr, (const uint8_t *)message, strlen(message));
    }
}
