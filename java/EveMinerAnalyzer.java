import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.util.*;
import java.util.regex.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * EVE Online Strip Miner Roll Analyzer
 * Reads item stats from clipboard and calculates m3/sec with mutation ranges
 * Supports both ORE Strip Miner and Modulated Strip Miner II
 * 
 * Version: 1.1.1
 */
public class EveMinerAnalyzer extends JFrame {
    
    private static final String VERSION = "1.1.1";
    private static final String APP_NAME = "EVE Online Strip Miner Roll Analyzer";
    
    // ============================================================================
    // BASE STATS AND CONFIGURATION
    // ============================================================================
    
    private static final Map<String, Double> ORE_BASE_STATS = new HashMap<>();
    private static final Map<String, Double> MODULATED_BASE_STATS = new HashMap<>();
    
    private static final Map<String, Map<String, Double>> ORE_TIER_RANGES = new HashMap<>();
    private static final Map<String, Map<String, Double>> MODULATED_TIER_RANGES = new HashMap<>();
    
    static {
        // ORE Base Stats
        ORE_BASE_STATS.put("ActivationCost", 23.0);
        ORE_BASE_STATS.put("StructureHitpoints", 40.0);
        ORE_BASE_STATS.put("Volume", 5.0);
        ORE_BASE_STATS.put("OptimalRange", 18.75);
        ORE_BASE_STATS.put("ActivationTime", 45.0);
        ORE_BASE_STATS.put("MiningAmount", 200.0);
        ORE_BASE_STATS.put("CriticalSuccessChance", 0.01);
        ORE_BASE_STATS.put("ResidueVolumeMultiplier", 0.0);
        ORE_BASE_STATS.put("ResidueProbability", 0.0);
        ORE_BASE_STATS.put("TechLevel", 1.0);
        ORE_BASE_STATS.put("CriticalSuccessBonusYield", 2.0);
        ORE_BASE_STATS.put("MetaLevel", 6.0);
        
        // Modulated Base Stats
        MODULATED_BASE_STATS.put("ActivationCost", 30.0);
        MODULATED_BASE_STATS.put("StructureHitpoints", 40.0);
        MODULATED_BASE_STATS.put("Volume", 5.0);
        MODULATED_BASE_STATS.put("Capacity", 10.0);
        MODULATED_BASE_STATS.put("OptimalRange", 15.00);
        MODULATED_BASE_STATS.put("ActivationTime", 45.0);
        MODULATED_BASE_STATS.put("MiningAmount", 120.0);
        MODULATED_BASE_STATS.put("CriticalSuccessChance", 0.01);
        MODULATED_BASE_STATS.put("ResidueVolumeMultiplier", 1.0);
        MODULATED_BASE_STATS.put("ResidueProbability", 0.34);
        MODULATED_BASE_STATS.put("TechLevel", 2.0);
        MODULATED_BASE_STATS.put("CriticalSuccessBonusYield", 2.0);
        MODULATED_BASE_STATS.put("MetaLevel", 5.0);
        
        // ORE Tier Ranges
        Map<String, Double> sRange = new HashMap<>();
        sRange.put("Min", 6.27);
        sRange.put("Max", 6.61);
        ORE_TIER_RANGES.put("S", sRange);
        
        Map<String, Double> aRange = new HashMap<>();
        aRange.put("Min", 5.92);
        aRange.put("Max", 6.27);
        ORE_TIER_RANGES.put("A", aRange);
        
        Map<String, Double> bRange = new HashMap<>();
        bRange.put("Min", 5.57);
        bRange.put("Max", 5.92);
        ORE_TIER_RANGES.put("B", bRange);
        
        Map<String, Double> cRange = new HashMap<>();
        cRange.put("Min", 5.23);
        cRange.put("Max", 5.57);
        ORE_TIER_RANGES.put("C", cRange);
        
        Map<String, Double> dRange = new HashMap<>();
        dRange.put("Min", 4.88);
        dRange.put("Max", 5.23);
        ORE_TIER_RANGES.put("D", dRange);
        
        Map<String, Double> eRange = new HashMap<>();
        eRange.put("Min", 4.44);
        eRange.put("Max", 4.88);
        ORE_TIER_RANGES.put("E", eRange);
        
        Map<String, Double> fRange = new HashMap<>();
        fRange.put("Min", 0.0);
        fRange.put("Max", 4.44);
        ORE_TIER_RANGES.put("F", fRange);
        
        // Modulated Tier Ranges
        Map<String, Double> modSRange = new HashMap<>();
        modSRange.put("Min", 3.76188);
        modSRange.put("Max", 3.97);
        MODULATED_TIER_RANGES.put("S", modSRange);
        
        Map<String, Double> modARange = new HashMap<>();
        modARange.put("Min", 3.55376);
        modARange.put("Max", 3.76188);
        MODULATED_TIER_RANGES.put("A", modARange);
        
        Map<String, Double> modBRange = new HashMap<>();
        modBRange.put("Min", 3.34564);
        modBRange.put("Max", 3.55376);
        MODULATED_TIER_RANGES.put("B", modBRange);
        
        Map<String, Double> modCRange = new HashMap<>();
        modCRange.put("Min", 3.13752);
        modCRange.put("Max", 3.34564);
        MODULATED_TIER_RANGES.put("C", modCRange);
        
        Map<String, Double> modDRange = new HashMap<>();
        modDRange.put("Min", 2.92940);
        modDRange.put("Max", 3.13752);
        MODULATED_TIER_RANGES.put("D", modDRange);
        
        Map<String, Double> modERange = new HashMap<>();
        modERange.put("Min", 2.67);
        modERange.put("Max", 2.92940);
        MODULATED_TIER_RANGES.put("E", modERange);
        
        Map<String, Double> modFRange = new HashMap<>();
        modFRange.put("Min", 0.0);
        modFRange.put("Max", 2.67);
        MODULATED_TIER_RANGES.put("F", modFRange);
    }
    
    private static final double SHIP_ROLE_BONUS = 1.75;
    private static final double MODULE_BONUS = 1.15;
    private static final double MINING_FOREMAN_BURST_YIELD = 1.15;
    private static final double INDUSTRIAL_CORE_YIELD = 1.50;
    private static final double INDUSTRIAL_CORE_CYCLE_TIME = 0.75;
    private static final double CALIBRATION_MULTIPLIER = 1.35;
    
    // UI Components
    private JRadioButton oreRadio;
    private JRadioButton modulatedRadio;
    private JLabel statusLabel;
    private JTextPane resultsText;
    private StyledDocument doc;
    
    private String minerType = "ORE";
    private String lastClipboardHash = null;
    private Timer clipboardTimer;
    
    // Theme colors
    private Color bgColor;
    private Color fgColor;
    private Color frameBg;
    private Map<String, Color> tierColors;
    private boolean isDarkMode;
    private Boolean manualThemeOverride = null; // null = auto, true = dark, false = light
    
    // UI Components for theme updates
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel typePanel;
    private JMenuBar menuBar;
    
    public EveMinerAnalyzer() {
        try {
            detectSystemTheme();
        } catch (Exception e) {
            // If theme detection fails, use default light theme
            isDarkMode = false;
            applyTheme();
        }
        initializeUI();
        startClipboardMonitoring();
    }
    
    private void detectSystemTheme() {
        // Try to detect system theme
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Use default if detection fails
        }
        
        // Check if manual override is set
        if (manualThemeOverride != null) {
            isDarkMode = manualThemeOverride;
        } else {
            // Auto-detect system theme
            isDarkMode = detectSystemDarkMode();
        }
        
        applyTheme();
    }
    
    private boolean detectSystemDarkMode() {
        // Method 1: Try Windows Registry (most reliable for Windows)
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme"
                );
                Process process = pb.start();
                
                // Wait with timeout (2 seconds max) - compatible with Java 7+
                boolean finished = false;
                try {
                    // Try Java 8+ method first
                    finished = process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                } catch (NoSuchMethodError e) {
                    // Fallback for Java 7: use simple waitFor with thread interrupt
                    Thread waitThread = new Thread(() -> {
                        try {
                            process.waitFor();
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    waitThread.start();
                    try {
                        waitThread.join(2000); // Wait max 2 seconds
                        finished = !waitThread.isAlive();
                    } catch (InterruptedException ie) {
                        waitThread.interrupt();
                        Thread.currentThread().interrupt();
                    }
                }
                
                if (!finished) {
                    try {
                        process.destroy();
                    } catch (Exception e) {
                        // Ignore destroy errors
                    }
                    // Timeout - fall through to other detection methods
                } else {
                    java.io.BufferedReader reader = null;
                    try {
                        reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream())
                        );
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("AppsUseLightTheme")) {
                                // If value is 0, dark mode is enabled
                                if (line.contains("0x0") || line.matches(".*\\s0\\s*$")) {
                                    return true;
                                }
                            }
                        }
                    } finally {
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (Exception e) {
                                // Ignore close errors
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Registry read failed, try other methods
            }
        }
        
        // Method 2: Check UI Manager colors (fallback)
        try {
            Color bg = UIManager.getColor("Panel.background");
            if (bg != null) {
                // Dark mode typically has low brightness
                double brightness = (bg.getRed() * 0.299 + bg.getGreen() * 0.587 + bg.getBlue() * 0.114) / 255.0;
                if (brightness < 0.5) {
                    return true;
                }
            }
        } catch (Exception e) {
            // Color check failed
        }
        
        // Method 3: Check system property (if available)
        try {
            String theme = System.getProperty("swing.systemTheme");
            if (theme != null && theme.toLowerCase().contains("dark")) {
                return true;
            }
        } catch (Exception e) {
            // Property check failed
        }
        
        // Default to light mode
        return false;
    }
    
    private void applyTheme() {
        tierColors = new HashMap<>();
        if (isDarkMode) {
            bgColor = new Color(30, 30, 30);
            fgColor = Color.WHITE;
            frameBg = new Color(45, 45, 45);
            tierColors.put("S", new Color(0, 255, 0));
            tierColors.put("A", new Color(0, 255, 255));
            tierColors.put("B", new Color(0, 128, 255));
            tierColors.put("C", Color.YELLOW);
            tierColors.put("D", new Color(255, 0, 255));
            tierColors.put("E", new Color(255, 165, 0));
            tierColors.put("F", Color.RED);
        } else {
            bgColor = Color.WHITE;
            fgColor = Color.BLACK;
            frameBg = new Color(240, 240, 240);
            tierColors.put("S", new Color(0, 170, 0));
            tierColors.put("A", new Color(0, 170, 170));
            tierColors.put("B", new Color(0, 102, 204));
            tierColors.put("C", new Color(204, 170, 0));
            tierColors.put("D", new Color(170, 0, 170));
            tierColors.put("E", new Color(204, 102, 0));
            tierColors.put("F", new Color(204, 0, 0));
        }
        
        // Update UI if already initialized
        if (mainPanel != null) {
            updateUITheme();
        }
    }
    
    private void updateUITheme() {
        SwingUtilities.invokeLater(() -> {
            // Update main panel
            if (mainPanel != null) {
                mainPanel.setBackground(bgColor);
            }
            
            // Update header panel
            if (headerPanel != null) {
                headerPanel.setBackground(bgColor);
            }
            
            // Update type panel
            if (typePanel != null) {
                typePanel.setBackground(bgColor);
            }
            
            // Update labels and nested components
            updateComponentTheme(getContentPane());
            
            // Update radio buttons
            if (oreRadio != null) {
                oreRadio.setBackground(bgColor);
                oreRadio.setForeground(fgColor);
            }
            if (modulatedRadio != null) {
                modulatedRadio.setBackground(bgColor);
                modulatedRadio.setForeground(fgColor);
            }
            
            // Update status label
            if (statusLabel != null) {
                statusLabel.setForeground(fgColor);
            }
            
            // Update text pane
            if (resultsText != null) {
                resultsText.setBackground(frameBg);
                resultsText.setForeground(fgColor);
            }
            
            // Rebuild text styles
            if (doc != null) {
                setupTextStyles();
            }
            
            // Repaint
            repaint();
        });
    }
    
    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label != statusLabel) { // statusLabel is handled separately
                    label.setForeground(fgColor);
                }
            } else if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                panel.setBackground(bgColor);
                updateComponentTheme(panel);
            } else if (comp instanceof JScrollPane) {
                updateComponentTheme((Container) comp);
            }
        }
    }
    
    private void toggleTheme() {
        if (manualThemeOverride == null) {
            // Currently auto, switch to opposite of current
            manualThemeOverride = !isDarkMode;
        } else {
            // Currently manual, toggle
            manualThemeOverride = !manualThemeOverride;
        }
        detectSystemTheme();
    }
    
    private void setThemeAuto() {
        manualThemeOverride = null;
        detectSystemTheme();
    }
    
    private void setThemeLight() {
        manualThemeOverride = false;
        detectSystemTheme();
    }
    
    private void setThemeDark() {
        manualThemeOverride = true;
        detectSystemTheme();
    }
    
    private void initializeUI() {
        setTitle(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        
        // Add menu bar with Theme and Help menus
        menuBar = new JMenuBar();
        
        // Theme menu
        JMenu themeMenu = new JMenu("Theme");
        
        JMenuItem toggleThemeItem = new JMenuItem("Toggle Theme");
        toggleThemeItem.addActionListener(e -> toggleTheme());
        themeMenu.add(toggleThemeItem);
        
        themeMenu.addSeparator();
        
        JMenuItem autoThemeItem = new JMenuItem("Auto (Follow System)");
        autoThemeItem.addActionListener(e -> setThemeAuto());
        themeMenu.add(autoThemeItem);
        
        JMenuItem lightThemeItem = new JMenuItem("Light");
        lightThemeItem.addActionListener(e -> setThemeLight());
        themeMenu.add(lightThemeItem);
        
        JMenuItem darkThemeItem = new JMenuItem("Dark");
        darkThemeItem.addActionListener(e -> setThemeDark());
        themeMenu.add(darkThemeItem);
        
        menuBar.add(themeMenu);
        
        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
        
        // Main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(bgColor);
        
        // Header panel
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(bgColor);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("EVE Online Strip Miner Roll Analyzer");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(fgColor);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Miner type selection
        typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.setBackground(bgColor);
        typePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        JLabel typeLabel = new JLabel("Miner Type:");
        typeLabel.setForeground(fgColor);
        typePanel.add(typeLabel);
        
        ButtonGroup typeGroup = new ButtonGroup();
        oreRadio = new JRadioButton("ORE", true);
        oreRadio.setBackground(bgColor);
        oreRadio.setForeground(fgColor);
        oreRadio.addActionListener(e -> {
            minerType = "ORE";
            clearResults();
        });
        typeGroup.add(oreRadio);
        typePanel.add(oreRadio);
        
        modulatedRadio = new JRadioButton("Modulated");
        modulatedRadio.setBackground(bgColor);
        modulatedRadio.setForeground(fgColor);
        modulatedRadio.addActionListener(e -> {
            minerType = "Modulated";
            clearResults();
        });
        typeGroup.add(modulatedRadio);
        typePanel.add(modulatedRadio);
        
        headerPanel.add(typePanel, BorderLayout.CENTER);
        
        // Status label
        statusLabel = new JLabel("Monitoring clipboard...");
        statusLabel.setForeground(fgColor);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        headerPanel.add(statusLabel, BorderLayout.SOUTH);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Results panel
        resultsText = new JTextPane();
        resultsText.setEditable(false);
        resultsText.setBackground(frameBg);
        resultsText.setForeground(fgColor);
        resultsText.setFont(new Font("Consolas", Font.PLAIN, 10));
        doc = resultsText.getStyledDocument();
        
        JScrollPane scrollPane = new JScrollPane(resultsText);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Setup text styles
        setupTextStyles();
        
        add(mainPanel);
        
        // Initial message
        appendText("Miner type set to: ORE\n", fgColor);
        appendText("Copy item stats from EVE Online to analyze.\n", fgColor);
    }
    
    private void setupTextStyles() {
        if (doc == null) {
            return; // Document not initialized yet
        }
        
        if (tierColors == null || tierColors.isEmpty()) {
            return; // Tier colors not initialized yet
        }
        
        try {
            // Tier colors - get or create styles
            for (Map.Entry<String, Color> entry : tierColors.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                String styleName = "tier_" + entry.getKey();
                Style style = doc.getStyle(styleName);
                if (style == null) {
                    style = doc.addStyle(styleName, null);
                }
                if (style != null) {
                    StyleConstants.setForeground(style, entry.getValue());
                    StyleConstants.setBold(style, true);
                }
            }
            
            // Good/Bad colors - get or create styles
            Color sColor = tierColors.get("S");
            if (sColor != null) {
                Style goodStyle = doc.getStyle("good");
                if (goodStyle == null) {
                    goodStyle = doc.addStyle("good", null);
                }
                if (goodStyle != null) {
                    StyleConstants.setForeground(goodStyle, sColor);
                }
            }
            
            Color fColor = tierColors.get("F");
            if (fColor != null) {
                Style badStyle = doc.getStyle("bad");
                if (badStyle == null) {
                    badStyle = doc.addStyle("bad", null);
                }
                if (badStyle != null) {
                    StyleConstants.setForeground(badStyle, fColor);
                }
            }
            
            // Header style - get or create
            Style headerStyle = doc.getStyle("header");
            if (headerStyle == null) {
                headerStyle = doc.addStyle("header", null);
            }
            if (headerStyle != null) {
                StyleConstants.setBold(headerStyle, true);
                StyleConstants.setFontSize(headerStyle, 11);
            }
        } catch (Exception e) {
            // Silently handle style errors - don't break the app
            // Don't print stack trace to avoid console spam
        }
    }
    
    private void clearResults() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
                appendText("Miner type set to: " + minerType + "\n", fgColor);
                appendText("Copy item stats from EVE Online to analyze.\n", fgColor);
                updateStatus("Miner type changed. Waiting for clipboard update...");
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void appendText(String text, Color color) {
        try {
            Style style = doc.addStyle("temp", null);
            StyleConstants.setForeground(style, color);
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void appendStyledText(String text, String styleName) {
        try {
            Style style = doc.getStyle(styleName);
            if (style == null) {
                style = doc.addStyle(styleName, null);
            }
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            statusLabel.setText(message + " - " + timestamp);
        });
    }
    
    // ============================================================================
    // CALCULATION FUNCTIONS
    // ============================================================================
    
    private static double getSkillBonus() {
        double miningBonus = 1 + (5 * 0.05);  // 1.25x
        double astroBonus = 1 + (5 * 0.05);    // 1.25x
        double exhumerBonus = 1 + (5 * 0.05);  // 1.25x
        return miningBonus * astroBonus * exhumerBonus;
    }
    
    private static double calculateBaseM3PerSec(double miningAmount, double activationTime) {
        if (activationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        return miningAmount / activationTime;
    }
    
    private static double calculateEffectiveM3PerSec(double miningAmount, double activationTime,
                                                     double critChance, double critBonus,
                                                     double residueProbability, double residueMultiplier) {
        if (activationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        
        double baseM3 = miningAmount;
        double critGain = baseM3 * critBonus * critChance;
        double residueLoss = baseM3 * residueProbability * residueMultiplier;
        double expectedM3PerCycle = baseM3 + critGain - residueLoss;
        return expectedM3PerCycle / activationTime;
    }
    
    private static double calculateBasePlusCritsM3PerSec(double miningAmount, double activationTime,
                                                        double critChance, double critBonus) {
        double baseM3 = miningAmount;
        double critGain = baseM3 * critBonus * critChance;
        double expectedM3PerCycle = baseM3 + critGain;
        return expectedM3PerCycle / activationTime;
    }
    
    private static double calculateRealWorldBaseM3PerSec(double baseMiningAmount, double baseActivationTime) {
        if (baseActivationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        
        double skillMultiplier = getSkillBonus();
        double bonusedMiningAmount = baseMiningAmount * skillMultiplier;
        bonusedMiningAmount = bonusedMiningAmount * SHIP_ROLE_BONUS;
        bonusedMiningAmount = bonusedMiningAmount * MODULE_BONUS;
        
        double boostedMiningAmount = bonusedMiningAmount * MINING_FOREMAN_BURST_YIELD * INDUSTRIAL_CORE_YIELD;
        double boostedActivationTime = baseActivationTime * INDUSTRIAL_CORE_CYCLE_TIME;
        
        if (boostedActivationTime <= 0) {
            throw new IllegalArgumentException("Boosted ActivationTime cannot be zero or negative");
        }
        
        boostedMiningAmount = boostedMiningAmount * CALIBRATION_MULTIPLIER;
        return boostedMiningAmount / boostedActivationTime;
    }
    
    private static double calculateRealWorldEffectiveM3PerSec(double baseMiningAmount, double baseActivationTime,
                                                              double critChance, double critBonus,
                                                              double residueProbability, double residueMultiplier) {
        if (baseActivationTime <= 0) {
            throw new IllegalArgumentException("ActivationTime cannot be zero or negative");
        }
        
        double skillMultiplier = getSkillBonus();
        double bonusedMiningAmount = baseMiningAmount * skillMultiplier;
        bonusedMiningAmount = bonusedMiningAmount * SHIP_ROLE_BONUS;
        bonusedMiningAmount = bonusedMiningAmount * MODULE_BONUS;
        
        double boostedMiningAmount = bonusedMiningAmount * MINING_FOREMAN_BURST_YIELD * INDUSTRIAL_CORE_YIELD;
        double boostedActivationTime = baseActivationTime * INDUSTRIAL_CORE_CYCLE_TIME;
        
        if (boostedActivationTime <= 0) {
            throw new IllegalArgumentException("Boosted ActivationTime cannot be zero or negative");
        }
        
        boostedMiningAmount = boostedMiningAmount * CALIBRATION_MULTIPLIER;
        
        double baseM3 = boostedMiningAmount;
        double critGain = baseM3 * critBonus * critChance;
        double residueLoss = baseM3 * residueProbability * residueMultiplier;
        double expectedM3PerCycle = baseM3 + critGain - residueLoss;
        return expectedM3PerCycle / boostedActivationTime;
    }
    
    // ============================================================================
    // PARSING FUNCTIONS
    // ============================================================================
    
    private static Map<String, Double> parseItemStats(String clipboardText) {
        Map<String, Double> stats = new HashMap<>();
        if (clipboardText == null || clipboardText.trim().isEmpty()) {
            return stats;
        }
        
        String[] lines = clipboardText.split("\n");
        
        // Try multiple patterns for better compatibility
        // Pattern 1: Tab-separated (most common from EVE)
        Pattern pattern1 = Pattern.compile("^(.+?)\\t+(\\d+\\.?\\d*)");
        // Pattern 2: Space-separated
        Pattern pattern2 = Pattern.compile("^(.+?)\\s+(\\d+\\.?\\d*)");
        // Pattern 3: Multiple spaces
        Pattern pattern3 = Pattern.compile("^(.+?)\\s{2,}(\\d+\\.?\\d*)");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            Matcher matcher = null;
            boolean found = false;
            
            // Try different patterns in order
            matcher = pattern1.matcher(line);
            if (matcher.find()) {
                found = true;
            } else {
                matcher = pattern2.matcher(line);
                if (matcher.find()) {
                    found = true;
                } else {
                    matcher = pattern3.matcher(line);
                    if (matcher.find()) {
                        found = true;
                    }
                }
            }
            
            if (found && matcher != null) {
                try {
                    String label = matcher.group(1).trim();
                    String valueStr = matcher.group(2).trim();
                    // Remove any trailing units or characters (keep decimal point)
                    valueStr = valueStr.replaceAll("[^0-9.]", "");
                    if (valueStr.isEmpty()) continue;
                    
                    double numValue = Double.parseDouble(valueStr);
                    String labelLower = label.toLowerCase();
                    
                    if (labelLower.matches("^activation\\s+cost.*")) {
                        stats.put("ActivationCost", numValue);
                    } else if (labelLower.matches(".*activation\\s+time.*") || 
                              (labelLower.matches(".*duration.*") && !labelLower.contains("residue"))) {
                        stats.put("ActivationTime", numValue);
                    } else if (labelLower.matches("^mining\\s+amount.*")) {
                        stats.put("MiningAmount", numValue);
                    } else if (labelLower.matches(".*critical\\s+success\\s+chance.*")) {
                        stats.put("CriticalSuccessChance", numValue / 100.0);
                    } else if (labelLower.matches(".*critical\\s+success\\s+bonus\\s+yield.*")) {
                        stats.put("CriticalSuccessBonusYield", numValue / 100.0);
                    } else if (labelLower.matches("^optimal\\s+range.*")) {
                        stats.put("OptimalRange", numValue);
                    } else if (labelLower.matches(".*residue\\s+probability.*")) {
                        stats.put("ResidueProbability", numValue / 100.0);
                    } else if (labelLower.matches(".*residue\\s+volume\\s+multiplier.*")) {
                        stats.put("ResidueVolumeMultiplier", numValue);
                    }
                } catch (NumberFormatException e) {
                    // Skip lines that can't be parsed - this is normal
                    continue;
                }
            }
        }
        
        return stats;
    }
    
    // ============================================================================
    // ANALYSIS FUNCTIONS
    // ============================================================================
    
    private static class AnalysisResult {
        Map<String, Double> stats;
        double m3PerSec;
        Double basePlusCritsM3PerSec;
        double effectiveM3PerSec;
        double realWorldM3PerSec;
        Double realWorldBasePlusCritsM3PerSec;
        double realWorldEffectiveM3PerSec;
        String tier;
    }
    
    private AnalysisResult analyzeRoll(Map<String, Double> stats, Map<String, Double> baseStats, String minerType) {
        AnalysisResult result = new AnalysisResult();
        
        // Fill in defaults from base stats
        Map<String, Double> mutatedStats = new HashMap<>(baseStats);
        mutatedStats.putAll(stats);
        result.stats = mutatedStats;
        
        // Calculate m3/sec
        result.m3PerSec = calculateBaseM3PerSec(mutatedStats.get("MiningAmount"), mutatedStats.get("ActivationTime"));
        
        // Calculate effective m3/sec
        if ("ORE".equals(minerType)) {
            result.effectiveM3PerSec = calculateEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                0.0, 0.0
            );
            result.basePlusCritsM3PerSec = null;
        } else {
            result.basePlusCritsM3PerSec = calculateBasePlusCritsM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield")
            );
            result.effectiveM3PerSec = calculateEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                mutatedStats.get("ResidueProbability"),
                mutatedStats.get("ResidueVolumeMultiplier")
            );
        }
        
        // Calculate real-world values
        result.realWorldM3PerSec = calculateRealWorldBaseM3PerSec(
            mutatedStats.get("MiningAmount"),
            mutatedStats.get("ActivationTime")
        );
        
        if ("ORE".equals(minerType)) {
            result.realWorldEffectiveM3PerSec = calculateRealWorldEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                0.0, 0.0
            );
            result.realWorldBasePlusCritsM3PerSec = null;
        } else {
            result.realWorldBasePlusCritsM3PerSec = calculateRealWorldEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                0.0, 0.0
            );
            result.realWorldEffectiveM3PerSec = calculateRealWorldEffectiveM3PerSec(
                mutatedStats.get("MiningAmount"),
                mutatedStats.get("ActivationTime"),
                mutatedStats.get("CriticalSuccessChance"),
                mutatedStats.get("CriticalSuccessBonusYield"),
                mutatedStats.get("ResidueProbability"),
                mutatedStats.get("ResidueVolumeMultiplier")
            );
        }
        
                // Determine tier
                Map<String, Map<String, Double>> tierRanges = "ORE".equals(minerType) ? ORE_TIER_RANGES : MODULATED_TIER_RANGES;
                result.tier = "F";
                
                if (tierRanges != null) {
                    Map<String, Double> sRange = tierRanges.get("S");
                    Map<String, Double> aRange = tierRanges.get("A");
                    Map<String, Double> bRange = tierRanges.get("B");
                    Map<String, Double> cRange = tierRanges.get("C");
                    Map<String, Double> dRange = tierRanges.get("D");
                    Map<String, Double> eRange = tierRanges.get("E");
                    Map<String, Double> fRange = tierRanges.get("F");
                    
                    if (sRange != null && sRange.get("Min") != null && result.m3PerSec >= sRange.get("Min")) {
                        result.tier = "S";
                    } else if (aRange != null && aRange.get("Min") != null && aRange.get("Max") != null && 
                               result.m3PerSec >= aRange.get("Min") && result.m3PerSec < aRange.get("Max")) {
                        result.tier = "A";
                    } else if (bRange != null && bRange.get("Min") != null && bRange.get("Max") != null && 
                               result.m3PerSec >= bRange.get("Min") && result.m3PerSec < bRange.get("Max")) {
                        result.tier = "B";
                    } else if (cRange != null && cRange.get("Min") != null && cRange.get("Max") != null && 
                               result.m3PerSec >= cRange.get("Min") && result.m3PerSec < cRange.get("Max")) {
                        result.tier = "C";
                    } else if (dRange != null && dRange.get("Min") != null && dRange.get("Max") != null && 
                               result.m3PerSec >= dRange.get("Min") && result.m3PerSec < dRange.get("Max")) {
                        result.tier = "D";
                    } else if (eRange != null && eRange.get("Min") != null && eRange.get("Max") != null && 
                               result.m3PerSec >= eRange.get("Min") && result.m3PerSec < eRange.get("Max")) {
                        result.tier = "E";
                    } else if (fRange != null && fRange.get("Max") != null && result.m3PerSec < fRange.get("Max")) {
                        result.tier = "F";
                    }
                }
        
        return result;
    }
    
    // ============================================================================
    // DISPLAY FUNCTIONS
    // ============================================================================
    
    private String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    private String formatPercentage(double value) {
        if (value > 0.1) {
            return String.format("+%.1f%%", value);
        } else if (value < -0.1) {
            return String.format("%.1f%%", value);
        } else {
            return "+0.0%";
        }
    }
    
    private String getColorTag(double value) {
        if (value > 0.1) {
            return "good";
        } else if (value < -0.1) {
            return "bad";
        }
        return null;
    }
    
    private void displayAnalysis(AnalysisResult analysis, Map<String, Double> baseStats, String minerType) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (doc == null || analysis == null || baseStats == null || minerType == null) {
                    return;
                }
                
                doc.remove(0, doc.getLength());
                
                Map<String, Double> stats = analysis.stats;
                if (stats == null) {
                    return;
                }
                
                // Header
                appendStyledText(repeat("=", 76) + "\n", "header");
                appendStyledText("EVE Online " + minerType + " Strip Miner Roll Analyzer\n", "header");
                appendStyledText(repeat("=", 76) + "\n\n", "header");
                
                // Roll Analysis
                appendStyledText("Roll Analysis:\n", "header");
                appendText(String.format("%-20s %-20s %-20s %-20s\n", "Metric", "Base", "Rolled", "% Change"), fgColor);
                appendText(repeat("-", 76) + "\n", fgColor);
                
                // Mining Amount
                double miningMut = ((stats.get("MiningAmount") / baseStats.get("MiningAmount")) - 1) * 100;
                String tag = getColorTag(miningMut);
                appendText(String.format("%-20s ", "Mining Amount"), fgColor);
                appendText(String.format("%.1f m3%12s ", baseStats.get("MiningAmount"), ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.1f m3%12s ", stats.get("MiningAmount"), ""), tag);
                    appendStyledText(formatPercentage(miningMut) + "\n", tag);
                } else {
                    appendText(String.format("%.1f m3%12s ", stats.get("MiningAmount"), ""), fgColor);
                    appendText(formatPercentage(miningMut) + "\n", fgColor);
                }
                
                // Activation Time
                double timeMut = ((stats.get("ActivationTime") / baseStats.get("ActivationTime")) - 1) * 100;
                tag = getColorTag(-timeMut);
                appendText(String.format("%-20s ", "Activation Time"), fgColor);
                appendText(String.format("%.1f s%13s ", baseStats.get("ActivationTime"), ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.1f s%13s ", stats.get("ActivationTime"), ""), tag);
                    appendStyledText(formatPercentage(timeMut) + "\n", tag);
                } else {
                    appendText(String.format("%.1f s%13s ", stats.get("ActivationTime"), ""), fgColor);
                    appendText(formatPercentage(timeMut) + "\n", fgColor);
                }
                
                // Crit Chance
                double critChanceMut = baseStats.get("CriticalSuccessChance") > 0 ?
                    ((stats.get("CriticalSuccessChance") / baseStats.get("CriticalSuccessChance")) - 1) * 100 : 0;
                tag = getColorTag(critChanceMut);
                appendText(String.format("%-20s ", "Crit Chance"), fgColor);
                appendText(String.format("%.2f%%%15s ", baseStats.get("CriticalSuccessChance") * 100, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f%%%15s ", stats.get("CriticalSuccessChance") * 100, ""), tag);
                    appendStyledText(formatPercentage(critChanceMut) + "\n", tag);
                } else {
                    appendText(String.format("%.2f%%%15s ", stats.get("CriticalSuccessChance") * 100, ""), fgColor);
                    appendText(formatPercentage(critChanceMut) + "\n", fgColor);
                }
                
                // Crit Bonus
                double critBonusMut = ((stats.get("CriticalSuccessBonusYield") / baseStats.get("CriticalSuccessBonusYield")) - 1) * 100;
                tag = getColorTag(critBonusMut);
                appendText(String.format("%-20s ", "Crit Bonus"), fgColor);
                appendText(String.format("%.0f%%%16s ", baseStats.get("CriticalSuccessBonusYield") * 100, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.0f%%%16s ", stats.get("CriticalSuccessBonusYield") * 100, ""), tag);
                    appendStyledText(formatPercentage(critBonusMut) + "\n", tag);
                } else {
                    appendText(String.format("%.0f%%%16s ", stats.get("CriticalSuccessBonusYield") * 100, ""), fgColor);
                    appendText(formatPercentage(critBonusMut) + "\n", fgColor);
                }
                
                // Residue (Modulated only)
                if ("Modulated".equals(minerType)) {
                    double residueProbMut = ((stats.get("ResidueProbability") / baseStats.get("ResidueProbability")) - 1) * 100;
                    tag = getColorTag(-residueProbMut);
                    appendText(String.format("%-20s ", "Residue Prob"), fgColor);
                    appendText(String.format("%.2f%%%15s ", baseStats.get("ResidueProbability") * 100, ""), fgColor);
                    if (tag != null) {
                        appendStyledText(String.format("%.2f%%%15s ", stats.get("ResidueProbability") * 100, ""), tag);
                        appendStyledText(formatPercentage(residueProbMut) + "\n", tag);
                    } else {
                        appendText(String.format("%.2f%%%15s ", stats.get("ResidueProbability") * 100, ""), fgColor);
                        appendText(formatPercentage(residueProbMut) + "\n", fgColor);
                    }
                    
                    double residueMultMut = ((stats.get("ResidueVolumeMultiplier") / baseStats.get("ResidueVolumeMultiplier")) - 1) * 100;
                    tag = getColorTag(-residueMultMut);
                    appendText(String.format("%-20s ", "Residue Mult"), fgColor);
                    appendText(String.format("%.3f x%14s ", baseStats.get("ResidueVolumeMultiplier"), ""), fgColor);
                    if (tag != null) {
                        appendStyledText(String.format("%.3f x%14s ", stats.get("ResidueVolumeMultiplier"), ""), tag);
                        appendStyledText(formatPercentage(residueMultMut) + "\n", tag);
                    } else {
                        appendText(String.format("%.3f x%14s ", stats.get("ResidueVolumeMultiplier"), ""), fgColor);
                        appendText(formatPercentage(residueMultMut) + "\n", fgColor);
                    }
                }
                
                // Optimal Range
                double optimalRangeMut = baseStats.get("OptimalRange") > 0 ?
                    ((stats.get("OptimalRange") / baseStats.get("OptimalRange")) - 1) * 100 : 0;
                tag = getColorTag(optimalRangeMut);
                appendText(String.format("%-20s ", "Optimal Range"), fgColor);
                appendText(String.format("%.2f km%14s ", baseStats.get("OptimalRange"), ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f km%14s ", stats.get("OptimalRange"), ""), tag);
                    appendStyledText(formatPercentage(optimalRangeMut) + "\n", tag);
                } else {
                    appendText(String.format("%.2f km%14s ", stats.get("OptimalRange"), ""), fgColor);
                    appendText(formatPercentage(optimalRangeMut) + "\n", fgColor);
                }
                
                appendText("\n", fgColor);
                
                // Performance Metrics
                appendStyledText("Performance Metrics:\n", "header");
                appendText(String.format("%-20s %-20s %-20s %-20s\n", "Metric", "Base", "Rolled", "% Change"), fgColor);
                appendText(repeat("-", 76) + "\n", fgColor);
                
                // Calculate base values
                double baseM3PerSec = calculateBaseM3PerSec(baseStats.get("MiningAmount"), baseStats.get("ActivationTime"));
                double baseEffectiveM3PerSec = calculateEffectiveM3PerSec(
                    baseStats.get("MiningAmount"),
                    baseStats.get("ActivationTime"),
                    baseStats.get("CriticalSuccessChance"),
                    baseStats.get("CriticalSuccessBonusYield"),
                    baseStats.getOrDefault("ResidueProbability", 0.0),
                    baseStats.getOrDefault("ResidueVolumeMultiplier", 0.0)
                );
                double baseRealWorldM3PerSec = calculateRealWorldBaseM3PerSec(
                    baseStats.get("MiningAmount"),
                    baseStats.get("ActivationTime")
                );
                double baseRealWorldEffectiveM3PerSec = calculateRealWorldEffectiveM3PerSec(
                    baseStats.get("MiningAmount"),
                    baseStats.get("ActivationTime"),
                    baseStats.get("CriticalSuccessChance"),
                    baseStats.get("CriticalSuccessBonusYield"),
                    baseStats.getOrDefault("ResidueProbability", 0.0),
                    baseStats.getOrDefault("ResidueVolumeMultiplier", 0.0)
                );
                
                // Base M3/sec
                double baseM3Pct = baseM3PerSec > 0 ? ((analysis.m3PerSec / baseM3PerSec) - 1) * 100 : 0;
                tag = getColorTag(baseM3Pct);
                appendText(String.format("%-20s ", "Base M3/sec"), fgColor);
                appendText(String.format("%.2f (%.1f)%6s ", baseM3PerSec, baseRealWorldM3PerSec, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f (%.1f)%6s ", analysis.m3PerSec, analysis.realWorldM3PerSec, ""), tag);
                    appendStyledText(formatPercentage(baseM3Pct) + "\n", tag);
                } else {
                    appendText(String.format("%.2f (%.1f)%6s ", analysis.m3PerSec, analysis.realWorldM3PerSec, ""), fgColor);
                    appendText(formatPercentage(baseM3Pct) + "\n", fgColor);
                }
                
                // Base + Crits (Modulated only)
                if ("Modulated".equals(minerType) && analysis.basePlusCritsM3PerSec != null) {
                    double baseBasePlusCrits = calculateBasePlusCritsM3PerSec(
                        baseStats.get("MiningAmount"),
                        baseStats.get("ActivationTime"),
                        baseStats.get("CriticalSuccessChance"),
                        baseStats.get("CriticalSuccessBonusYield")
                    );
                    double baseRealWorldBasePlusCrits = calculateRealWorldEffectiveM3PerSec(
                        baseStats.get("MiningAmount"),
                        baseStats.get("ActivationTime"),
                        baseStats.get("CriticalSuccessChance"),
                        baseStats.get("CriticalSuccessBonusYield"),
                        0.0, 0.0
                    );
                    double basePlusCritsPct = baseBasePlusCrits > 0 ?
                        ((analysis.basePlusCritsM3PerSec / baseBasePlusCrits) - 1) * 100 : 0;
                    tag = getColorTag(basePlusCritsPct);
                    appendText(String.format("%-20s ", "Base + Crits M3/s"), fgColor);
                    appendText(String.format("%.2f (%.1f)%6s ", baseBasePlusCrits, baseRealWorldBasePlusCrits, ""), fgColor);
                    if (tag != null) {
                        appendStyledText(String.format("%.2f (%.1f)%6s ", analysis.basePlusCritsM3PerSec, 
                            analysis.realWorldBasePlusCritsM3PerSec, ""), tag);
                        appendStyledText(formatPercentage(basePlusCritsPct) + "\n", tag);
                    } else {
                        appendText(String.format("%.2f (%.1f)%6s ", analysis.basePlusCritsM3PerSec, 
                            analysis.realWorldBasePlusCritsM3PerSec, ""), fgColor);
                        appendText(formatPercentage(basePlusCritsPct) + "\n", fgColor);
                    }
                }
                
                // Effective M3/sec
                double effM3Pct = baseEffectiveM3PerSec > 0 ?
                    ((analysis.effectiveM3PerSec / baseEffectiveM3PerSec) - 1) * 100 : 0;
                tag = getColorTag(effM3Pct);
                appendText(String.format("%-20s ", "Effective M3/sec"), fgColor);
                appendText(String.format("%.2f (%.1f)%6s ", baseEffectiveM3PerSec, baseRealWorldEffectiveM3PerSec, ""), fgColor);
                if (tag != null) {
                    appendStyledText(String.format("%.2f (%.1f)%6s ", analysis.effectiveM3PerSec, 
                        analysis.realWorldEffectiveM3PerSec, ""), tag);
                    appendStyledText(formatPercentage(effM3Pct) + "\n", tag);
                } else {
                    appendText(String.format("%.2f (%.1f)%6s ", analysis.effectiveM3PerSec, 
                        analysis.realWorldEffectiveM3PerSec, ""), fgColor);
                    appendText(formatPercentage(effM3Pct) + "\n", fgColor);
                }
                
                appendText("\n", fgColor);
                
                // Tier
                String tier = analysis.tier;
                if (tier == null) {
                    tier = "F";
                }
                Map<String, Map<String, Double>> tierRanges = "ORE".equals(minerType) ? ORE_TIER_RANGES : MODULATED_TIER_RANGES;
                Map<String, Double> tierRange = tierRanges != null ? tierRanges.get(tier) : null;
                
                String tierRangeStr = "";
                if (tierRange != null) {
                    Double min = tierRange.get("Min");
                    Double max = tierRange.get("Max");
                    if (min != null && max != null) {
                        if ("S".equals(tier)) {
                            tierRangeStr = String.format("%.2f-%.2f+ m³/s", min, max);
                        } else if ("F".equals(tier)) {
                            tierRangeStr = String.format("<%.2f m³/s", max);
                        } else {
                            tierRangeStr = String.format("%.2f-%.5f m³/s", min, max);
                        }
                    }
                }
                
                appendStyledText("Tier: ", "header");
                appendStyledText(tier + "\n", "tier_" + tier);
                if (!tierRangeStr.isEmpty()) {
                    appendStyledText("(" + tierRangeStr + ")\n", "tier_" + tier);
                }
                
                appendText("\n" + repeat("=", 76) + "\n", fgColor);
                
                // Copy to clipboard
                String tierDisplay = "S".equals(tier) ? "+S" : tier;
                String minerLabel = "ORE".equals(minerType) ? "[ORE]" : "[Modulated]";
                String clipboardText = tierDisplay + ": (" + formatPercentage(baseM3Pct) + ") " + minerLabel;
                try {
                    StringSelection selection = new StringSelection(clipboardText);
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, null);
                } catch (Exception e) {
                    // Ignore clipboard errors
                }
                
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    // ============================================================================
    // CLIPBOARD MONITORING
    // ============================================================================
    
    private void startClipboardMonitoring() {
        clipboardTimer = new Timer(true);
        clipboardTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkClipboard();
            }
        }, 0, 300); // Check every 300ms
    }
    
    private void checkClipboard() {
        try {
            // Check if we're in a headless environment
            if (GraphicsEnvironment.isHeadless()) {
                return;
            }
            
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            if (clipboard == null) {
                return;
            }
            
            Transferable contents = null;
            try {
                contents = clipboard.getContents(null);
            } catch (IllegalStateException e) {
                // Clipboard is locked by another application
                return;
            } catch (SecurityException e) {
                // Security exception - can't access clipboard
                SwingUtilities.invokeLater(() -> {
                    if (statusLabel != null) {
                        updateStatus("Clipboard access denied - check permissions");
                    }
                });
                return;
            }
            
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String clipboardText = null;
                try {
                    clipboardText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                } catch (UnsupportedFlavorException e) {
                    // Clipboard doesn't contain text - this is normal, ignore
                    return;
                } catch (java.io.IOException e) {
                    // IO error reading clipboard
                    return;
                }
                
                if (clipboardText != null && !clipboardText.isEmpty()) {
                    String currentHash = String.valueOf(clipboardText.hashCode());
                    
                    if (!currentHash.equals(lastClipboardHash)) {
                        lastClipboardHash = currentHash;
                        
                        // Parse stats
                        Map<String, Double> parsedStats = parseItemStats(clipboardText);
                        
                        if (!parsedStats.isEmpty()) {
                            Map<String, Double> baseStats = "ORE".equals(minerType) ? ORE_BASE_STATS : MODULATED_BASE_STATS;
                            
                            if (baseStats == null || baseStats.isEmpty()) {
                                SwingUtilities.invokeLater(() -> {
                                    if (statusLabel != null) {
                                        updateStatus("Error: Base stats not initialized");
                                    }
                                });
                                return;
                            }
                            
                            try {
                                // Analyze
                                AnalysisResult analysis = analyzeRoll(parsedStats, baseStats, minerType);
                                
                                if (analysis != null) {
                                    // Display
                                    displayAnalysis(analysis, baseStats, minerType);
                                    updateStatus("Analysis complete");
                                }
                            } catch (IllegalArgumentException e) {
                                SwingUtilities.invokeLater(() -> {
                                    if (doc != null) {
                                        appendText("\nError in calculation: " + e.getMessage() + "\n", Color.RED);
                                    }
                                    if (statusLabel != null) {
                                        updateStatus("Calculation error: " + e.getMessage());
                                    }
                                });
                            } catch (NullPointerException e) {
                                SwingUtilities.invokeLater(() -> {
                                    if (doc != null) {
                                        appendText("\nError: Missing required data in clipboard\n", Color.RED);
                                    }
                                    if (statusLabel != null) {
                                        updateStatus("Error: Missing data");
                                    }
                                });
                            } catch (Exception e) {
                                SwingUtilities.invokeLater(() -> {
                                    if (doc != null) {
                                        appendText("\nUnexpected error: " + e.getMessage() + "\n", Color.RED);
                                    }
                                    if (statusLabel != null) {
                                        updateStatus("Error during analysis");
                                    }
                                });
                            }
                        } else {
                            // Clipboard changed but no stats parsed - might be non-miner data
                            SwingUtilities.invokeLater(() -> {
                                if (statusLabel != null) {
                                    updateStatus("Clipboard updated but no miner stats found");
                                }
                            });
                        }
                    }
                }
            }
        } catch (HeadlessException e) {
            // Running in headless environment - can't access GUI
            return;
        } catch (Exception e) {
            // Log error but don't spam the UI - only show occasionally
            // Use a simple counter to avoid spamming
            if (Math.random() < 0.01) { // Only show 1% of errors to avoid spam
                SwingUtilities.invokeLater(() -> {
                    if (statusLabel != null) {
                        updateStatus("Error reading clipboard: " + e.getClass().getSimpleName());
                    }
                });
            }
        }
    }
    
    private void showAboutDialog() {
        String message = APP_NAME + "\n" +
                        "Version " + VERSION + "\n\n" +
                        "Analyzes EVE Online Strip Miner rolls by monitoring clipboard.\n" +
                        "Supports ORE Strip Miner and Modulated Strip Miner II.\n\n" +
                        "Usage:\n" +
                        "1. Select miner type (ORE or Modulated)\n" +
                        "2. Copy item stats from EVE Online (Ctrl+C on item info)\n" +
                        "3. Analysis appears automatically\n\n" +
                        "Tier info is automatically copied to clipboard for easy container naming.";
        
        JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    // ============================================================================
    // MAIN
    // ============================================================================
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Use default LAF
            }
            
            new EveMinerAnalyzer().setVisible(true);
        });
    }
}


