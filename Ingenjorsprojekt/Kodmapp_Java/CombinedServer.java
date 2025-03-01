import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.swing.*;

public class CombinedServer {
    // Karta
    private static final char EMPTY = '-';
    private static final char SMOKE = 'R';
    private static final char FIRE = 'F';
    private static final char WALL = '#';
    private static final char FIREFIGHTER = 'B';
    private static final int WIDTH = 8;
    private static final int HEIGHT = 6;
    private static final int CELL_SIZE = 2;
    private static char[][] map = new char[HEIGHT * CELL_SIZE][WIDTH * CELL_SIZE];

    private final Random random = new Random();
    private Map<String, int[]> firefighterPositions = new HashMap<>();
    // För meddelanden från gateway
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    // Seriell port
    private SerialPort serialPort;
    private OutputStream outputStream;

    // till GUI
    private JFrame frame;
    private Map<String, StringBuilder> firefighterHistory = new HashMap<>(); // Lagrar firefighters historik här
    private JTextArea eventArea, firefighterArea; // Delar upp gui i olika delar.
    private JList<String> firefighterList; // Lista på alla firefirghets.
    private DefaultListModel<String> listModel;
    // Till Start GUI
    private JFrame startFrame;
    private JButton startButton;
    // Bilder
    private static ImageIcon smokeIcon;
    private static ImageIcon fireIcon;
    private static ImageIcon wallIcon;
    private static ImageIcon firefighterIcon;
    private static JPanel gridPanel;

    private volatile boolean paused = false; // Pause flag



    // Konstruktor
    public CombinedServer() {
        initializeMap();
        initMap(); // Initierar tom karta
        clearLogFile(); // Raderar innehållet i log.txt
        initSerialPort(); // Initierar seriella kommunikation med gateaway
        setupGui(); // Set up the GUI components
        createStartScreen();
        // new Thread(this::simulate).start(); // Simulerar kartan, uppdaterar den med
        // rök/bränder
        // new Thread(this::readFromSerialPort).start(); // Läser från seriellporten

    }

    private static void initializeMap() {
        map = new char[HEIGHT * CELL_SIZE][WIDTH * CELL_SIZE];
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                map[y][x] = EMPTY;
            }
        }
    }

    // Start screen
    private void createStartScreen() {
        // Skapa ramen för startskärmen
        startFrame = new JFrame("Firefighter Simulation");
        startFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        startFrame.setSize(600, 400);
        startFrame.setLayout(new BorderLayout());

        // Skapa en knapp och gör den blå
        startButton = new JButton("Start Game");
        startButton.setFont(new Font("Arial", Font.BOLD, 19));
        startButton.setBackground(Color.BLUE);
        startButton.setForeground(Color.BLACK);

        // Lägg till en bakgrundsbild
        JLabel background = new JLabel();
        background.setLayout(new GridBagLayout()); // För centrering av knappen
        try {
            Image img = ImageIO.read(new File("Kodmapp_Java/image/pngimg.com - firefighter_PNG15884.png")); // Ange
                                                                                                            // sökvägen
                                                                                                            // till din
                                                                                                            // bild
            background.setIcon(new ImageIcon(img));
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Lägg till knappen på bakgrunden
        background.add(startButton);

        // Registrera en lyssnare för startknappen
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });

        // Lägg till bakgrund på startfönstret
        startFrame.add(background);
        startFrame.setVisible(true);
    }

    // Startar simulering när man trycker på start.
    private void startGame() {
        // Stäng Start-skärmen
        startFrame.setVisible(false);

        // Skicka OK till gatewayen för att starta spelet
        sendStartSignalToGateway();

        // Visa huvudserverfönstret
        frame.setVisible(true);

        // Börja eventuella simuleringstrådar efter att spelet startats
        new Thread(this::readFromSerialPort).start();
        new Thread(this::simulateMap).start(); // Startar kartan i terminalen uppdateras varje

        new Thread(this::simulate).start();
    }

    private void sendStartSignalToGateway() {
        if (outputStream != null) {
            try {
                String startMessage = "START:OK\n";
                outputStream.write(startMessage.getBytes());
                outputStream.flush();
                System.out.println("Start signal sent to gateway: " + startMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Initierar en tom karta
    private void initMap() {
        for (int y = 0; y < HEIGHT * CELL_SIZE; y++) {
            for (int x = 0; x < WIDTH * CELL_SIZE; x++) {
                map[y][x] = EMPTY;
            }
        }

        // Hårdkoda väggar, behövs ej för tillfället.
        // setRegion(WALL, 1, 1);
        // setRegion(WALL, 1, 2);
        // setRegion(WALL, 2, 1);

    }

    // En region i kartan
    private void setRegion(char value, int regionX, int regionY) {
        for (int y = 0; y < CELL_SIZE; y++) {
            for (int x = 0; x < CELL_SIZE; x++) {
                map[regionY * CELL_SIZE + y][regionX * CELL_SIZE + x] = value;
            }
        }
    }

    // GUI som loggar allt
    private void setupGui() {

        try {
            smokeIcon = new ImageIcon("Kodmapp_Java/image/ound-png-2.png");
            fireIcon = new ImageIcon("Kodmapp_Java/image/firewwww.png");
            wallIcon = new ImageIcon("Kodmapp_Java/image/wall.png");
            firefighterIcon = new ImageIcon("Kodmapp_Java/image/firefightrrt-2.png");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Kunde inte ladda ikonerna.");
            System.exit(1);
        }
        frame = new JFrame("Firefighter Simulation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(5000, 800);
        frame.setLayout(new BorderLayout());

        // Skapa ett JLayeredPane för att hantera lager
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setPreferredSize(new Dimension(800, 600));

        try {
            BufferedImage image = ImageIO.read(new File("Kodmapp_Java/image/flashpoint_industry.png"));
            JLabel imageLabel = new JLabel(new ImageIcon(image));
            imageLabel.setBounds(0, 0, image.getWidth(), image.getHeight());
            layeredPane.add(imageLabel, Integer.valueOf(0)); // Bakgrundslager
        } catch (Exception e) {
            e.printStackTrace();
            JLabel errorLabel = new JLabel("Kunde inte ladda bilden.");
            errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
            errorLabel.setBounds(0, 0, 800, 600); // Se till att errorLabel täcker hela ytan
            layeredPane.add(errorLabel, Integer.valueOf(0));
        }
        
              // Skapa rutnät för kartan och lägg till det i det övre lagret
              gridPanel = new JPanel();
              gridPanel.setLayout(new GridLayout(HEIGHT * CELL_SIZE, WIDTH * CELL_SIZE));
              gridPanel.setOpaque(false); // Gör rutnätet transparent
              gridPanel.setBounds(0, 0, 800, 600); // Anpassa rutnätets storlek till fönstret

              for (int y = 0; y < map.length; y++) {
                for (int x = 0; x < map[0].length; x++) {
                    JLabel cell = new JLabel("", SwingConstants.CENTER);
                    cell.setOpaque(false); // Gör celler transparenta
                    cell.setBackground(getColor(map[y][x]));
                    gridPanel.add(cell);
                }
            }
            layeredPane.add(gridPanel, Integer.valueOf(1)); // Övre lagret

                  // Uppdatera kartan regelbundet BEHÖVS EJ.
       // new Timer(2000, e -> {
        //    updateMap();
         //   renderMap();
       // }).start();

            frame.add(layeredPane);
            frame.pack();
            frame.setVisible(false);
        // Initialize text areas
        eventArea = new JTextArea();
        firefighterArea = new JTextArea();
        eventArea.setEditable(false);
        firefighterArea.setEditable(false);

        // Initialize the firefighter list
        listModel = new DefaultListModel<>();
        firefighterList = new JList<>(listModel);
        firefighterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Define fonts
        Font boldFont = new Font("Arial", Font.BOLD, 12);

        // Create panels with titles for event and firefighter logs
        JPanel eventsPanel = new JPanel(new BorderLayout());
        JLabel eventsTitle = new JLabel("          Events:            ");
        eventsTitle.setFont(boldFont);
        eventsTitle.setHorizontalAlignment(SwingConstants.CENTER);
        eventsPanel.add(eventsTitle, BorderLayout.NORTH);
        eventsPanel.add(new JScrollPane(eventArea), BorderLayout.CENTER);

        JPanel firefightersPanel = new JPanel(new BorderLayout());
        JLabel firefightersTitle = new JLabel("           Firefighters:            ");
        firefightersTitle.setFont(boldFont);
        firefightersTitle.setHorizontalAlignment(SwingConstants.CENTER);
        firefightersPanel.add(firefightersTitle, BorderLayout.NORTH);
        firefightersPanel.add(new JScrollPane(firefighterArea), BorderLayout.CENTER);

        // Composite panel for the left side containing events and firefighter logs
        JPanel componentsPanel = new JPanel(new GridLayout(2, 1));
        componentsPanel.add(eventsPanel);
        componentsPanel.add(firefightersPanel);

        // Panel for the right side containing active firefighters list
        JPanel rightPanel = new JPanel(new BorderLayout());
        JLabel activeFirefightersLabel = new JLabel("Active Firefighters");
        activeFirefightersLabel.setFont(boldFont);
        activeFirefightersLabel.setHorizontalAlignment(SwingConstants.CENTER);
        rightPanel.add(activeFirefightersLabel, BorderLayout.NORTH);
        rightPanel.add(new JScrollPane(firefighterList), BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(150, 600));

        // Control panel with buttons to pause and resume simulation
        JPanel controlPanel = new JPanel(new GridLayout(2, 1));
        JButton pauseSimulationButton = new JButton("Pause Simulation");
        JButton resumeSimulationButton = new JButton("Resume Simulation");

        pauseSimulationButton.addActionListener(e -> pauseSimulation());
        resumeSimulationButton.addActionListener(e -> resumeSimulation());

        controlPanel.add(pauseSimulationButton);
        controlPanel.add(resumeSimulationButton);
        controlPanel.setPreferredSize(new Dimension(150, 100));

        // Add components to frame
        frame.add(componentsPanel, BorderLayout.WEST);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.add(controlPanel, BorderLayout.SOUTH);

        // Handle selection of firefighters to show history
        firefighterList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    String selectedFirefighter = firefighterList.getSelectedValue();
                    if (selectedFirefighter != null) {
                        showFirefighterHistory(selectedFirefighter);
                    }
                }
            }
        });
    }

    private void pauseSimulation() {
        paused = true;
        System.out.println("Simulation paused.");
    }

    private synchronized void resumeSimulation() {
        paused = false;
        System.out.println("Simulation resumed.");
        notifyAll(); // Notify threads that may be waiting
    }
  

    private static void renderMap() {
        Component[] components = gridPanel.getComponents();
        for (int y = 0; y < map.length; y++) {
            for (int x = 0; x < map[0].length; x++) {
                int index = y * map[0].length + x;
                JLabel cell = (JLabel) components[index];
                switch (map[y][x]) {
                    case SMOKE -> cell.setIcon(smokeIcon);
                    case FIRE -> cell.setIcon(fireIcon);
                    case WALL -> cell.setIcon(wallIcon);
                    case FIREFIGHTER -> cell.setIcon(firefighterIcon);
                    default -> cell.setIcon(null); // Ingen ikon för EMPTY
                }
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private static Color getColor(char cellType) {
        return switch (cellType) {
            //case SMOKE -> cell.setIcon(smokeIcon);
            case FIRE -> Color.RED;
            case WALL -> Color.BLACK;
            case FIREFIGHTER -> Color.BLUE;
            default -> new Color(0, 0, 0, 0); // Transparent för EMPTY
        };
    }


    // Visar en brandmans historik.
    private void showFirefighterHistory(String id) {
        JFrame historyFrame = new JFrame("History for Firefighter: " + id);
        historyFrame.setSize(200, 300);
        JTextArea historyArea = new JTextArea();
        historyArea.setEditable(false);
        if (firefighterHistory.containsKey(id)) {
            historyArea.setText(firefighterHistory.get(id).toString());
        } else {
            historyArea.setText("No history available");
        }
        historyFrame.add(new JScrollPane(historyArea));
        historyFrame.setVisible(true);
    }

    // Uppdaterar logg GUI med info
    private void updateLogGui() {
        try (BufferedReader reader = new BufferedReader(new FileReader("log.txt"))) {
            String line;
            StringBuilder eventLogs = new StringBuilder();
            StringBuilder firefighterLogs = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("Firefighter:")) {
                    String firefighterId = line.split(" ")[1];
                    firefighterLogs.append(line).append("\n");

                    if (!listModel.contains(firefighterId)) {
                        listModel.addElement(firefighterId);
                    }
                } else if (line.contains("Smoke") || line.contains("Fire")) {
                    eventLogs.append(line).append("\n");
                }
            }

            eventArea.setText(eventLogs.toString());
            firefighterArea.setText(firefighterLogs.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // initierar seriell kommunikation med gateaway
    private void initSerialPort() {
        String desiredPort = "cu.usbserial-0001";
        SerialPort[] ports = SerialPort.getCommPorts();
        serialPort = null;

        System.out.println("Tillgängliga portar:");
        for (SerialPort port : ports) {
            System.out.println(port.getSystemPortName());
            if (port.getSystemPortName().equals(desiredPort)) {
                serialPort = port;
            }
        }
        if (serialPort == null) {
            System.err.println("Kunde inte hitta porten: " + desiredPort);
            return;
        }

        serialPort.setBaudRate(115200);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 1000);
        serialPort.setComPortParameters(115200, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);

        if (serialPort.openPort()) {
            System.out.println("Seriell port öppnad: " + serialPort.getSystemPortName());
            outputStream = serialPort.getOutputStream(); // Lägg till detta för att initiera OutputStream

        } else {
            System.err.println("Kunde inte öppna seriell port: " + desiredPort);
        }
    }

    // Hanterar meddelanden från gateway.
    private void processIncomingMessage(String message) {
        if (message.startsWith("UPDATE:")) {
            String[] parts = message.substring(7).split(",");
            if (parts.length == 5) {
                int regionX = Integer.parseInt(parts[0].trim());
                int regionY = Integer.parseInt(parts[1].trim());
                int cellX = Integer.parseInt(parts[2].trim());
                int cellY = Integer.parseInt(parts[3].trim());
                String mac = parts[4].trim();

                // Kontrollera om positionen är inom giltigt område
                if (isInBounds(regionX, regionY)) {
                    placeFirefighter(mac, regionX, regionY, cellX, cellY);

                }
            }
        }
    }

    private void placeFirefighter(String id, int globalX, int globalY, int cX, int cY) {
        int cellX = globalX * CELL_SIZE + cX;
        int cellY = globalY * CELL_SIZE + cY;
        // Kolla först om denna brandman redan har en position

        if (firefighterPositions.containsKey(id)) {
            int[] oldPos = firefighterPositions.get(id);
            int oldCellX = oldPos[0] * CELL_SIZE + oldPos[2]; // räkna ut den gamla X cell
            int oldCellY = oldPos[1] * CELL_SIZE + oldPos[3]; // räkna ut den gamla Y cell

            // Tar bort gamla positionen.
            if (map[oldCellY][oldCellX] == FIREFIGHTER) {
                map[oldCellY][oldCellX] = EMPTY;
            }
        }

        // Uppdatera med ny position om den inte är upptagen
        if (map[cellY][cellX] == EMPTY || map[cellY][cellX] == SMOKE) {
            map[cellY][cellX] = FIREFIGHTER;
            firefighterPositions.put(id, new int[] { globalX, globalY, cX, cY });

            // Uppdatera historik och logg
            firefighterHistory.putIfAbsent(id, new StringBuilder());
            firefighterHistory.get(id)
                    .append("Position at (" + globalX + ", " + globalY + "), cell (" + cX + ", " + cY + ")\n");

            logEvent("Firefighter: " + id, globalX, globalY, cX, cY);
            if (!listModel.contains(id)) {
                listModel.addElement(id); // Add ID to the JList model if not already present
            }
            updateLogGui();
            sendUpdateToGateway(FIREFIGHTER, globalX, globalY, cX, cY);
        }
    }

    private void simulateMap() {
        while (true) {
            synchronized (this) {
                while (paused) {
                    try {
                        wait(); // Pause the simulation
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }
            renderMap();
            try {
                Thread.sleep(500); // delay.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    // Uppdaterar karta med bränder, rök
    // Skickar kartan till gateaway
    // Och visar allt i terminalen.
    private void simulate() {
        while (true) {
            synchronized (this) {
                while (paused) {
                    try {
                        wait(); // Pause the simulation
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }
            updateMap();
            // sendMapToGateway();

            try {
                Thread.sleep(5000); // delay.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    // Uppdaterar kartan med brand, rök,
    // Loggar allt
    // Skickar även uppdatering till gateaway.
    private void updateMap() {
        int regionX = random.nextInt(WIDTH);
        int regionY = random.nextInt(HEIGHT);
        int subX = random.nextInt(CELL_SIZE);
        int subY = random.nextInt(CELL_SIZE);

        int cellX = regionX * CELL_SIZE + subX;
        int cellY = regionY * CELL_SIZE + subY;

        if (map[cellY][cellX] == EMPTY) {

            map[cellY][cellX] = SMOKE;
            sendUpdateToGateway(SMOKE, regionX, regionY, subX, subY);
            logEvent("Smoke", regionX, regionY, subX, subY);
        } else if (map[cellY][cellX] == SMOKE) {
            map[cellY][cellX] = FIRE;
            sendUpdateToGateway(FIRE, regionX, regionY, subX, subY);

            logEvent("Fire", regionX, regionY, subX, subY);
        }
        updateLogGui();
        // spreadFire();
    }

    // Sprider eld.
    private void spreadFire() {
        boolean[][] newFires = new boolean[HEIGHT * CELL_SIZE][WIDTH * CELL_SIZE];

        for (int y = 0; y < HEIGHT * CELL_SIZE; y++) {
            for (int x = 0; x < WIDTH * CELL_SIZE; x++) {
                if (map[y][x] == FIRE) {
                    spreadToAdjacent(x, y, newFires);
                }
            }
        }

        for (int y = 0; y < HEIGHT * CELL_SIZE; y++) {
            for (int x = 0; x < WIDTH * CELL_SIZE; x++) {
                if (newFires[y][x]) {
                    map[y][x] = FIRE;
                    logEvent("Fire spread", x / CELL_SIZE, y / CELL_SIZE, x % CELL_SIZE, y % CELL_SIZE);
                }
            }
        }
    }

    // Sprider eld till närliggande områden
    private void spreadToAdjacent(int x, int y, boolean[][] newFires) {
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

        for (int[] dir : directions) {
            int newX = x + dir[0];
            int newY = y + dir[1];

            if (isInBounds(newX, newY) && map[newY][newX] == EMPTY && random.nextBoolean()) {
                newFires[newY][newX] = true;
            }
        }
    }

    // Kollar så att brandmännen, rök, bränder inte placeras utanför kartan.
    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < WIDTH * CELL_SIZE && y >= 0 && y < HEIGHT * CELL_SIZE && map[y][x] != WALL;
    }

    // Skickar uppdateringar till gateaway
    private void sendUpdateToGateway(char type, int x, int y, int cX, int cY) {
        if (outputStream != null) {
            try {
                String updateMessage = String.format("%c:%d,%d,%d,%d\n", type, x, y, cX % CELL_SIZE, cY % CELL_SIZE); // Kontrollera
                                                                                                                      // detta
                outputStream.write(updateMessage.getBytes());
                outputStream.flush();
                System.out.println("Update sent to gateway: " + updateMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Loggar allt i log.txt filen
    private void logEvent(String event, int regionX, int regionY, int subX, int subY) {
        String message = event + " at region (" + regionX + ", " + regionY + "), cell (" + subX + ", " + subY + ")";
        System.out.println(message);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("log.txt", true))) {
            writer.write(message + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Tar bort innehållet i log.txt filen
    private static void clearLogFile() {
        try (PrintWriter writer = new PrintWriter("log.txt")) {
            System.out.println("Log file cleared.");
        } catch (IOException e) {
            System.err.println("Error clearing the log file: " + e.getMessage());
        }
    }
/* 
    // Skriver ut kartan i terminalen.
    private void renderMap() {
        System.out.print("\033[H\033[2J");
        System.out.flush();

        System.out.print("   ");
        for (int x = 0; x < WIDTH; x++) {
            System.out.printf(" %2d    ", x);
        }
        System.out.println();

        System.out.print("  +");
        for (int x = 0; x < WIDTH; x++) {
            System.out.print("------+");
        }
        System.out.println();

        for (int regionY = 0; regionY < HEIGHT; regionY++) {
            for (int subY = 0; subY < CELL_SIZE; subY++) {
                if (subY == 0) {
                    System.out.printf("%2d |", regionY);
                } else {
                    System.out.print("   |");
                }

                for (int regionX = 0; regionX < WIDTH; regionX++) {
                    for (int subX = 0; subX < CELL_SIZE; subX++) {
                        int globalX = regionX * CELL_SIZE + subX;
                        int globalY = regionY * CELL_SIZE + subY;
                        System.out.print(" " + map[globalY][globalX] + " ");
                    }
                    System.out.print("|");
                }
                System.out.println();
            }

            System.out.print("  +");
            for (int x = 0; x < WIDTH; x++) {
                System.out.print("------+");
            }
            System.out.println();
        }
    }
*/
    // Läser från porten
    private void readFromSerialPort() {
        if (serialPort == null) {
            System.err.println("Ingen seriell port är konfigurerad.");
            return;
        }
        try { // läset från port.
            byte[] buffer = new byte[1024];
            while (true) {
                if (serialPort.bytesAvailable() > 0) {
                    int bytesRead = serialPort.readBytes(buffer, buffer.length);
                    String received = new String(buffer, 0, bytesRead).trim();
                    messageQueue.add(received); // Lägger till meddelandet i kön.
                    processIncomingMessage(received); // Bearbeta meddelandet.

                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
        

    public static void main(String[] args) {
        new CombinedServer();
    }
}
