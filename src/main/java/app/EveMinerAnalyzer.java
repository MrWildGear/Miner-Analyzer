package app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import config.ConfigManager;
import config.OptimalRangeModifierManager;
import config.TierModifierManager;
import service.ClipboardMonitor;
import service.ErrorLogMonitor;
import ui.AnalysisDisplay;
import ui.ThemeManager;
import util.ErrorLogger;

/**
 * EVE Online Strip Miner Roll Analyzer Reads item stats from clipboard and calculates m3/sec with
 * mutation ranges Supports ORE Strip Miner, Modulated Strip Miner II, and ORE Ice Harvester
 * 
 */
public class EveMinerAnalyzer extends JFrame {

    private static final String VERSION = "1.4.15";
    private static final String APP_NAME = "EVE Online Strip Miner Roll Analyzer";
    private static final String DEFAULT_STYLE_NAME = "default";
    private static final String TIER_PREFIX = "Tier ";
    private static final String INVALID_INPUT = "Invalid Input";
    private static final String FONT_ARIAL = "Arial";
    private static final String ERROR_STATISTICS = "Error Statistics";

    // Validation constants
    private static final double MAX_ROLL_COST = 1e12; // Maximum allowed roll cost (1 trillion ISK)

    // UI Menu constants
    private static final String MENU_THEME = "Theme";
    private static final String MENU_SETTINGS = "Settings";
    private static final String MENU_HELP = "Help";
    private static final String MENU_ITEM_TOGGLE_THEME = "Toggle Theme";
    private static final String MENU_ITEM_AUTO_THEME = "Auto (Follow System)";
    private static final String MENU_ITEM_LIGHT = "Light";
    private static final String MENU_ITEM_DARK = "Dark";
    private static final String MENU_ITEM_ROLL_COST = "Roll Cost";
    private static final String MENU_ITEM_TIER_MODIFIERS = "Tier Modifiers";
    private static final String MENU_ITEM_ABOUT = "About";

    // Miner type constants
    private static final String MINER_TYPE_ORE = "ORE";
    private static final String MINER_TYPE_MODULATED = "Modulated";
    private static final String MINER_TYPE_ICE = "Ice";

    // UI Button constants
    private static final String BUTTON_COPY = "Copy";
    private static final String BUTTON_SAVE = "Save";
    private static final String BUTTON_CANCEL = "Cancel";

    // Font constants
    private static final String FONT_CONSOLAS = "Consolas";
    private static final int FONT_SIZE_RESULTS = 10;
    private static final int FONT_SIZE_TITLE = 16;

    // Window size constants
    private static final int WINDOW_WIDTH = 900;
    private static final int WINDOW_HEIGHT = 700;

    // Dialog constants
    private static final int DIALOG_WIDTH = 400;
    private static final int DIALOG_HEIGHT = 450;

    // Thread name constants
    private static final String THREAD_SHUTDOWN_HOOK = "ShutdownHook";

    // Status constants
    private static final String STATUS_ACTIVE = "Active";
    private static final String STATUS_INACTIVE = "Inactive";

    // UI Components
    private JRadioButton oreRadio;
    private JRadioButton modulatedRadio;
    private JRadioButton iceRadio;
    private JLabel statusLabel;
    private JTextPane resultsText;
    private transient StyledDocument doc;

    private String minerType = MINER_TYPE_ORE;
    private transient ClipboardMonitor clipboardMonitor;
    private transient ErrorLogMonitor errorLogMonitor;

    // Theme management
    private transient ThemeManager themeManager;
    private transient AnalysisDisplay analysisDisplay;

    // UI Components for theme updates
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel typePanel;
    private JMenuBar appMenuBar;

    // Sell price components
    private JPanel sellPricePanel;
    private JLabel sellPriceLabel;
    private JButton copySellPriceButton;
    private double currentSellPrice = 0.0;

    /**
     * Constructs the main application window and initializes the UI and clipboard monitoring.
     */
    public EveMinerAnalyzer() {
        themeManager = new ThemeManager();
        initializeUI();
        startClipboardMonitoring();
        startErrorLogMonitoring();

        // Register shutdown hook for graceful cleanup
        Runtime.getRuntime()
                .addShutdownHook(new Thread(this::performCleanup, THREAD_SHUTDOWN_HOOK));

        // Clean up on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                performCleanup();
                System.exit(0);
            }
        });
    }

    /**
     * Performs cleanup of resources (monitors, threads, etc.) during application shutdown. This
     * method is called both from the window closing event and from the shutdown hook.
     */
    private void performCleanup() {
        try {
            if (clipboardMonitor != null) {
                clipboardMonitor.stop();
            }
            if (errorLogMonitor != null) {
                errorLogMonitor.stop();
            }
        } catch (Exception ex) {
            ErrorLogger.logError("Error during application shutdown cleanup", ex);
        }
    }

    private void initializeUI() {
        setTitle(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);

        // Add menu bar with Theme and Help menus
        appMenuBar = new JMenuBar();

        // Theme menu
        JMenu themeMenu = new JMenu(MENU_THEME);

        JMenuItem toggleThemeItem = new JMenuItem(MENU_ITEM_TOGGLE_THEME);
        toggleThemeItem.addActionListener(e -> {
            themeManager.toggleTheme();
            updateUITheme();
        });
        themeMenu.add(toggleThemeItem);

        themeMenu.addSeparator();

        JMenuItem autoThemeItem = new JMenuItem(MENU_ITEM_AUTO_THEME);
        autoThemeItem.addActionListener(e -> {
            themeManager.setThemeAuto();
            updateUITheme();
        });
        themeMenu.add(autoThemeItem);

        JMenuItem lightThemeItem = new JMenuItem(MENU_ITEM_LIGHT);
        lightThemeItem.addActionListener(e -> {
            themeManager.setThemeLight();
            updateUITheme();
        });
        themeMenu.add(lightThemeItem);

        JMenuItem darkThemeItem = new JMenuItem(MENU_ITEM_DARK);
        darkThemeItem.addActionListener(e -> {
            themeManager.setThemeDark();
            updateUITheme();
        });
        themeMenu.add(darkThemeItem);

        appMenuBar.add(themeMenu);

        // Settings menu
        JMenu settingsMenu = new JMenu(MENU_SETTINGS);

        JMenuItem rollCostItem = new JMenuItem(MENU_ITEM_ROLL_COST);
        rollCostItem.addActionListener(e -> showRollCostDialog());
        settingsMenu.add(rollCostItem);

        settingsMenu.addSeparator();

        JMenuItem tierModifiersItem = new JMenuItem(MENU_ITEM_TIER_MODIFIERS);
        tierModifiersItem.addActionListener(e -> showTierModifiersDialog());
        settingsMenu.add(tierModifiersItem);

        appMenuBar.add(settingsMenu);

        // Help menu
        JMenu helpMenu = new JMenu(MENU_HELP);
        JMenuItem errorStatsItem = new JMenuItem(ERROR_STATISTICS);
        errorStatsItem.addActionListener(e -> showErrorStatisticsDialog());
        helpMenu.add(errorStatsItem);
        helpMenu.addSeparator();
        JMenuItem aboutItem = new JMenuItem(MENU_ITEM_ABOUT);
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        appMenuBar.add(helpMenu);

        // Style all menu items initially
        updateMenuBarColors(appMenuBar);

        setJMenuBar(appMenuBar);
        appMenuBar.setBackground(themeManager.getMenuBgColor());
        appMenuBar.setForeground(themeManager.getMenuFgColor());

        // Main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(themeManager.getBgColor());

        // Header panel
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getHeaderBgColor());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLabel = new JLabel(APP_NAME);
        titleLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, FONT_SIZE_TITLE));
        titleLabel.setForeground(themeManager.getFgColor());
        headerPanel.add(titleLabel, BorderLayout.NORTH);

        // Miner type selection
        typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        typePanel.setBackground(themeManager.getBgColor());
        typePanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel typeLabel = new JLabel("Miner Type:");
        typeLabel.setForeground(themeManager.getFgColor());
        typePanel.add(typeLabel);

        ButtonGroup typeGroup = new ButtonGroup();
        oreRadio = new JRadioButton(MINER_TYPE_ORE, true);
        oreRadio.setBackground(themeManager.getBgColor());
        oreRadio.setForeground(themeManager.getFgColor());
        oreRadio.addActionListener(e -> {
            minerType = MINER_TYPE_ORE;
            clearResults();
            restartClipboardMonitoring();
        });
        typeGroup.add(oreRadio);
        typePanel.add(oreRadio);

        modulatedRadio = new JRadioButton(MINER_TYPE_MODULATED);
        modulatedRadio.setBackground(themeManager.getBgColor());
        modulatedRadio.setForeground(themeManager.getFgColor());
        modulatedRadio.addActionListener(e -> {
            minerType = MINER_TYPE_MODULATED;
            clearResults();
            restartClipboardMonitoring();
        });
        typeGroup.add(modulatedRadio);
        typePanel.add(modulatedRadio);

        iceRadio = new JRadioButton(MINER_TYPE_ICE);
        iceRadio.setBackground(themeManager.getBgColor());
        iceRadio.setForeground(themeManager.getFgColor());
        iceRadio.addActionListener(e -> {
            minerType = MINER_TYPE_ICE;
            clearResults();
            restartClipboardMonitoring();
        });
        typeGroup.add(iceRadio);
        typePanel.add(iceRadio);

        headerPanel.add(typePanel, BorderLayout.CENTER);

        // Sell price panel
        sellPricePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sellPricePanel.setBackground(themeManager.getBgColor());
        sellPricePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        sellPriceLabel = new JLabel("Sell Price: -");
        sellPriceLabel.setForeground(themeManager.getFgColor());
        sellPricePanel.add(sellPriceLabel);

        copySellPriceButton = new JButton(BUTTON_COPY);
        copySellPriceButton.setEnabled(false);
        copySellPriceButton.addActionListener(e -> copySellPriceToClipboard());
        sellPricePanel.add(copySellPriceButton);

        // Status label
        statusLabel = new JLabel("Monitoring clipboard...");
        statusLabel.setForeground(themeManager.getFgColor());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Create a container panel to hold both sell price and status
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(themeManager.getHeaderBgColor());
        bottomPanel.add(sellPricePanel, BorderLayout.NORTH);
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        headerPanel.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Results panel
        resultsText = new JTextPane();
        resultsText.setEditable(false);
        resultsText.setBackground(themeManager.getFrameBg());
        resultsText.setForeground(themeManager.getFgColor());
        resultsText.setFont(new Font(FONT_CONSOLAS, Font.PLAIN, FONT_SIZE_RESULTS));
        resultsText.setCaretColor(themeManager.getFgColor()); // Ensure caret is visible
        doc = resultsText.getStyledDocument();

        // Set up default style for the document to ensure correct text color
        // Also set the logical style to use the correct foreground color
        try {
            javax.swing.text.Style defaultStyle = doc.getStyle(DEFAULT_STYLE_NAME);
            if (defaultStyle == null) {
                defaultStyle = doc.addStyle(DEFAULT_STYLE_NAME, null);
            }
            if (defaultStyle != null) {
                javax.swing.text.StyleConstants.setForeground(defaultStyle,
                        themeManager.getFgColor());
            }
            // Set logical style for the entire document
            doc.setLogicalStyle(0, defaultStyle);
        } catch (Exception e) {
            ErrorLogger.logError("Error setting up document style", e);
        }

        // Initialize analysis display
        analysisDisplay = new AnalysisDisplay(doc, themeManager.getFgColor(),
                themeManager.getTierColors(), this::updateSellPrice);

        JScrollPane scrollPane = new JScrollPane(resultsText);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        scrollPane.getViewport().setBackground(themeManager.getFrameBg());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Initial message
        analysisDisplay.appendText("Miner type set to: " + MINER_TYPE_ORE + "\n",
                themeManager.getFgColor());
        analysisDisplay.appendText("Copy item stats from EVE Online to analyze.\n",
                themeManager.getFgColor());
    }

    private void updateUITheme() {
        SwingUtilities.invokeLater(() -> {
            updateMenuBarTheme();
            updatePanelsTheme();
            updateComponentTheme(getContentPane());
            updateRadioButtonsTheme();
            updateStatusLabelTheme();
            updateTextPaneTheme();
            updateScrollPaneTheme();
            updateAnalysisDisplayTheme();
            repaint();
        });
    }

    private void updateMenuBarTheme() {
        if (appMenuBar != null) {
            appMenuBar.setBackground(themeManager.getMenuBgColor());
            appMenuBar.setForeground(themeManager.getMenuFgColor());
            updateMenuBarColors(appMenuBar);
        }
    }

    private void updatePanelsTheme() {
        if (mainPanel != null) {
            mainPanel.setBackground(themeManager.getBgColor());
        }
        if (headerPanel != null) {
            headerPanel.setBackground(themeManager.getHeaderBgColor());
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        }
        if (typePanel != null) {
            typePanel.setBackground(themeManager.getBgColor());
        }
        if (sellPricePanel != null) {
            sellPricePanel.setBackground(themeManager.getBgColor());
        }
        if (sellPriceLabel != null) {
            sellPriceLabel.setForeground(themeManager.getFgColor());
        }
        if (copySellPriceButton != null) {
            copySellPriceButton.setForeground(themeManager.getFgColor());
            copySellPriceButton.setBackground(themeManager.getBgColor());
        }
    }

    private void updateRadioButtonsTheme() {
        if (oreRadio != null) {
            oreRadio.setBackground(themeManager.getBgColor());
            oreRadio.setForeground(themeManager.getFgColor());
        }
        if (modulatedRadio != null) {
            modulatedRadio.setBackground(themeManager.getBgColor());
            modulatedRadio.setForeground(themeManager.getFgColor());
        }
        if (iceRadio != null) {
            iceRadio.setBackground(themeManager.getBgColor());
            iceRadio.setForeground(themeManager.getFgColor());
        }
    }

    private void updateStatusLabelTheme() {
        if (statusLabel != null) {
            statusLabel.setForeground(themeManager.getFgColor());
        }
    }

    private void updateTextPaneTheme() {
        if (resultsText == null) {
            return;
        }
        resultsText.setBackground(themeManager.getFrameBg());
        resultsText.setForeground(themeManager.getFgColor());
        resultsText.setCaretColor(themeManager.getFgColor());
        updateTextPaneStyle();
    }

    private void updateTextPaneStyle() {
        try {
            javax.swing.text.Style style =
                    resultsText.getStyledDocument().getStyle(DEFAULT_STYLE_NAME);
            if (style == null) {
                style = resultsText.getStyledDocument().addStyle(DEFAULT_STYLE_NAME, null);
            }
            if (style != null) {
                javax.swing.text.StyleConstants.setForeground(style, themeManager.getFgColor());
                resultsText.getStyledDocument().setLogicalStyle(0, style);
            }
        } catch (Exception e) {
            ErrorLogger.logError("Error updating text pane style", e);
        }
    }

    private void updateScrollPaneTheme() {
        if (mainPanel == null) {
            return;
        }
        Component[] components = mainPanel.getComponents();
        for (Component comp : components) {
            if (comp instanceof JScrollPane scrollPane) {
                scrollPane.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                scrollPane.getViewport().setBackground(themeManager.getFrameBg());
            }
        }
    }

    private void updateAnalysisDisplayTheme() {
        if (analysisDisplay != null) {
            analysisDisplay.updateTheme(themeManager.getFgColor(), themeManager.getTierColors());
        }
    }

    private void updateMenuBarColors(JMenuBar menuBar) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu != null) {
                menu.setForeground(themeManager.getMenuFgColor());
                menu.setBackground(themeManager.getMenuBgColor());
                menu.setOpaque(true);

                // Add popup menu listener to style popup when it appears
                menu.addMenuListener(new javax.swing.event.MenuListener() {
                    @Override
                    public void menuSelected(javax.swing.event.MenuEvent e) {
                        stylePopupMenu(menu);
                    }

                    @Override
                    public void menuDeselected(javax.swing.event.MenuEvent e) {
                        // No action needed when menu is deselected
                    }

                    @Override
                    public void menuCanceled(javax.swing.event.MenuEvent e) {
                        // No action needed when menu is canceled
                    }
                });

                // Style existing menu items
                for (int j = 0; j < menu.getItemCount(); j++) {
                    JMenuItem item = menu.getItem(j);
                    if (item != null) {
                        styleMenuItem(item);
                    }
                }
            }
        }
    }

    private void styleMenuItem(JMenuItem item) {
        item.setForeground(themeManager.getMenuFgColor());
        item.setBackground(themeManager.getMenuBgColor());
        item.setOpaque(true);

        // Handle submenus recursively
        if (item instanceof JMenu submenu) {
            for (int i = 0; i < submenu.getItemCount(); i++) {
                JMenuItem subItem = submenu.getItem(i);
                if (subItem != null) {
                    styleMenuItem(subItem);
                }
            }
        }
    }

    private void stylePopupMenu(JMenu menu) {
        javax.swing.JPopupMenu popup = menu.getPopupMenu();
        if (popup != null) {
            popup.setBackground(themeManager.getMenuBgColor());
            popup.setForeground(themeManager.getMenuFgColor());

            // Style all components in the popup
            Component[] components = popup.getComponents();
            for (Component comp : components) {
                if (comp instanceof JMenuItem menuItem) {
                    styleMenuItem(menuItem);
                }
            }
        }
    }

    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel label) {
                if (label != statusLabel) { // statusLabel is handled separately
                    label.setForeground(themeManager.getFgColor());
                }
            } else if (comp instanceof JPanel panel) {
                panel.setBackground(themeManager.getBgColor());
                updateComponentTheme(panel);
            } else if (comp instanceof JScrollPane scrollPane) {
                updateComponentTheme(scrollPane);
            }
        }
    }

    private void clearResults() {
        SwingUtilities.invokeLater(() -> {
            try {
                doc.remove(0, doc.getLength());
                analysisDisplay.appendText("Miner type set to: " + minerType + "\n",
                        themeManager.getFgColor());
                analysisDisplay.appendText("Copy item stats from EVE Online to analyze.\n",
                        themeManager.getFgColor());
                updateStatus("Miner type changed. Waiting for clipboard update...");
                // Reset sell price
                updateSellPrice(0.0);
            } catch (BadLocationException e) {
                ErrorLogger.logError("Error clearing results display", e);
            }
        });
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timeStr = java.time.LocalTime.now().toString();
                String timestamp = timeStr.length() >= 8 ? timeStr.substring(0, 8) : timeStr;
                statusLabel.setText(message + " - " + timestamp);
            } catch (Exception e) {
                // Fallback if timestamp formatting fails
                ErrorLogger.logError("Error updating status timestamp", e);
                statusLabel.setText(message);
            }
        });
    }

    private void startClipboardMonitoring() {
        if (clipboardMonitor != null) {
            clipboardMonitor.stop();
        }
        clipboardMonitor = new ClipboardMonitor(minerType, analysisDisplay, this::updateStatus);
        clipboardMonitor.start();
    }

    private void restartClipboardMonitoring() {
        startClipboardMonitoring();
    }

    private void startErrorLogMonitoring() {
        if (errorLogMonitor != null) {
            errorLogMonitor.stop();
        }
        errorLogMonitor = new ErrorLogMonitor((timestamp, message, errorNumber) ->
        // Optional: You can add notification logic here
        // For now, errors are tracked silently in the background
        SwingUtilities.invokeLater(() -> {
            // Could update status or show a notification here
        }));
        errorLogMonitor.start();
    }

    private void showRollCostDialog() {
        double currentCost = ConfigManager.getRollCost();

        String input = JOptionPane.showInputDialog(this,
                "Enter the cost to roll the mod (current: "
                        + (currentCost > 0 ? String.format("%.0f", currentCost) : "not set") + "):",
                "Roll Cost Settings", JOptionPane.QUESTION_MESSAGE);

        if (input != null && !input.trim().isEmpty()) {
            try {
                // Normalize numeric string to handle both thousand separators and decimal
                // separators
                String cleanedInput = ConfigManager.normalizeNumericString(input);
                double newCost = Double.parseDouble(cleanedInput);
                if (newCost < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Cost cannot be negative. Please enter a positive number.",
                            INVALID_INPUT, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (Double.isInfinite(newCost) || Double.isNaN(newCost)) {
                    JOptionPane.showMessageDialog(this,
                            "Invalid cost value. Please enter a valid number.", INVALID_INPUT,
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Reasonable upper bound: 1 trillion (1e12) to prevent extremely large values
                if (newCost > MAX_ROLL_COST) {
                    JOptionPane.showMessageDialog(this,
                            "Cost value too large. Please enter a value less than 1 trillion.",
                            INVALID_INPUT, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                ConfigManager.saveRollCost(newCost);
                JOptionPane.showMessageDialog(this,
                        "Roll cost set to " + String.format("%.0f", newCost), "Settings Saved",
                        JOptionPane.INFORMATION_MESSAGE);
                // Update sell price if analysis is displayed
                updateSellPriceIfNeeded();
            } catch (NumberFormatException e) {
                ErrorLogger.logError("Invalid roll cost input format", e);
                JOptionPane.showMessageDialog(this,
                        "Invalid number format. Please enter a valid decimal number.",
                        INVALID_INPUT, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showTierModifiersDialog() {
        // Load current modifiers from config
        Map<String, Double> currentModifiers = TierModifierManager.loadTierModifiers();
        double currentOptimalRangeModifier = OptimalRangeModifierManager.loadOptimalRangeModifier();
        String[] tiers = {"S", "A", "B", "C", "D", "E", "F"};

        // Create dialog
        JDialog dialog = new JDialog(this, "Tier Modifiers Settings", true);
        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(themeManager.getBgColor());

        // Create dialog panel
        JPanel dialogPanel = new JPanel(new java.awt.BorderLayout());
        dialogPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialogPanel.setBackground(themeManager.getBgColor());

        // Create main panel with scroll pane
        JPanel mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setBackground(themeManager.getBgColor());

        // Tier modifiers section
        JLabel tierSectionLabel = new JLabel("Tier Modifiers:");
        tierSectionLabel.setForeground(themeManager.getFgColor());
        tierSectionLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 12));
        tierSectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        mainContentPanel.add(tierSectionLabel);

        JPanel tierPanel = new JPanel();
        tierPanel.setLayout(new java.awt.GridLayout(tiers.length, 2, 5, 5));
        tierPanel.setBackground(themeManager.getBgColor());

        // Store text fields in maps for later retrieval
        Map<String, JTextField> tierTextFields = new java.util.HashMap<>();

        // Create labels and text fields for each tier
        for (String tier : tiers) {
            JLabel label = new JLabel(TIER_PREFIX + tier + ":");
            label.setForeground(themeManager.getFgColor());
            tierPanel.add(label);

            JTextField textField =
                    new JTextField(String.valueOf(currentModifiers.getOrDefault(tier, 1.0)));
            textField.setBackground(themeManager.getFrameBg());
            textField.setForeground(themeManager.getFgColor());
            tierTextFields.put(tier, textField);
            tierPanel.add(textField);
        }
        mainContentPanel.add(tierPanel);

        // Add spacing
        mainContentPanel.add(Box.createVerticalStrut(20));

        // Optimal Range Modifier section
        JLabel optimalRangeSectionLabel =
                new JLabel("Optimal Range Modifier (applies when tier has '+'):");
        optimalRangeSectionLabel.setForeground(themeManager.getFgColor());
        optimalRangeSectionLabel.setFont(new Font(FONT_ARIAL, Font.BOLD, 12));
        optimalRangeSectionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        mainContentPanel.add(optimalRangeSectionLabel);

        JPanel optimalRangePanel = new JPanel();
        optimalRangePanel.setLayout(new java.awt.GridLayout(1, 2, 5, 5));
        optimalRangePanel.setBackground(themeManager.getBgColor());

        JLabel optimalRangeLabel = new JLabel("Modifier:");
        optimalRangeLabel.setForeground(themeManager.getFgColor());
        optimalRangePanel.add(optimalRangeLabel);

        JTextField optimalRangeTextField =
                new JTextField(String.valueOf(currentOptimalRangeModifier));
        optimalRangeTextField.setBackground(themeManager.getFrameBg());
        optimalRangeTextField.setForeground(themeManager.getFgColor());
        optimalRangePanel.add(optimalRangeTextField);

        mainContentPanel.add(optimalRangePanel);

        // Wrap in scroll pane
        JScrollPane scrollPane = new JScrollPane(mainContentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        scrollPane.setBackground(themeManager.getBgColor());
        scrollPane.getViewport().setBackground(themeManager.getBgColor());

        dialogPanel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(themeManager.getBgColor());

        JButton saveButton = new JButton(BUTTON_SAVE);
        saveButton.addActionListener(
                e -> handleSaveTierModifiers(dialog, tierTextFields, optimalRangeTextField, tiers));

        JButton cancelButton = new JButton(BUTTON_CANCEL);
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        dialogPanel.add(buttonPanel, BorderLayout.SOUTH);

        dialog.add(dialogPanel);
        dialog.setVisible(true);
    }

    private void handleSaveTierModifiers(JDialog dialog, Map<String, JTextField> tierTextFields,
            JTextField optimalRangeTextField, String[] tiers) {
        ValidationResult result =
                validateTierModifiers(tierTextFields, optimalRangeTextField, tiers);

        if (result.isValid()) {
            TierModifierManager.saveTierModifiers(result.getTierModifiers());
            OptimalRangeModifierManager.saveOptimalRangeModifier(result.getOptimalRangeModifier());
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "Modifiers saved successfully.", "Settings Saved",
                    JOptionPane.INFORMATION_MESSAGE);
            updateSellPriceIfNeeded();
        } else {
            JOptionPane.showMessageDialog(dialog, result.getErrorMessage(), INVALID_INPUT,
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private ValidationResult validateTierModifiers(Map<String, JTextField> tierTextFields,
            JTextField optimalRangeTextField, String[] tiers) {
        Map<String, Double> newTierModifiers = new java.util.HashMap<>();
        StringBuilder errorMessage = new StringBuilder("Invalid values:\n");
        boolean hasError = false;

        // Validate tier modifiers
        for (String tier : tiers) {
            JTextField textField = tierTextFields.get(tier);
            String text = textField.getText().trim();
            if (text.isEmpty()) {
                hasError = true;
                errorMessage.append(TIER_PREFIX).append(tier).append(" cannot be empty\n");
                continue;
            }
            try {
                double value = Double.parseDouble(text);
                newTierModifiers.put(tier, value);
            } catch (NumberFormatException e) {
                ErrorLogger.logError("Invalid tier modifier format for tier: " + tier, e);
                hasError = true;
                errorMessage.append(TIER_PREFIX).append(tier).append(": invalid number format\n");
            }
        }

        // Validate optimal range modifier
        String optimalRangeText = optimalRangeTextField.getText().trim();
        double optimalRangeValue = 0.0;
        if (optimalRangeText.isEmpty()) {
            hasError = true;
            errorMessage.append("Optimal Range Modifier cannot be empty\n");
        } else {
            try {
                optimalRangeValue = Double.parseDouble(optimalRangeText);
                if (optimalRangeValue < 0) {
                    hasError = true;
                    errorMessage.append("Optimal Range Modifier cannot be negative\n");
                }
            } catch (NumberFormatException e) {
                ErrorLogger.logError("Invalid optimal range modifier format", e);
                hasError = true;
                errorMessage.append("Optimal Range Modifier: invalid number format\n");
            }
        }

        return new ValidationResult(!hasError, newTierModifiers, optimalRangeValue,
                errorMessage.toString());
    }

    private static class ValidationResult {
        private final boolean valid;
        private final Map<String, Double> tierModifiers;
        private final double optimalRangeModifier;
        private final String errorMessage;

        ValidationResult(boolean valid, Map<String, Double> tierModifiers,
                double optimalRangeModifier, String errorMessage) {
            this.valid = valid;
            this.tierModifiers = tierModifiers;
            this.optimalRangeModifier = optimalRangeModifier;
            this.errorMessage = errorMessage;
        }

        boolean isValid() {
            return valid;
        }

        Map<String, Double> getTierModifiers() {
            return tierModifiers;
        }

        double getOptimalRangeModifier() {
            return optimalRangeModifier;
        }

        String getErrorMessage() {
            return errorMessage;
        }
    }

    private void updateSellPriceIfNeeded() {
        // This will be handled by the next analysis
        // We just need to ensure the UI refreshes if there's a current analysis
    }

    /**
     * Updates the sell price display in the UI.
     * 
     * @param sellPrice The calculated sell price to display (in ISK)
     */
    public void updateSellPrice(double sellPrice) {
        SwingUtilities.invokeLater(() -> {
            currentSellPrice = sellPrice;
            if (sellPriceLabel != null) {
                if (sellPrice > 0) {
                    // Format with comma separators for readability
                    java.text.NumberFormat formatter = java.text.NumberFormat.getInstance();
                    formatter.setGroupingUsed(true);
                    formatter.setMaximumFractionDigits(0);
                    sellPriceLabel.setText("Sell Price: " + formatter.format(sellPrice) + " ISK");
                } else {
                    sellPriceLabel.setText("Sell Price: -");
                }
            }
            if (copySellPriceButton != null) {
                copySellPriceButton.setEnabled(sellPrice > 0);
            }
        });
    }

    private void copySellPriceToClipboard() {
        if (currentSellPrice > 0) {
            try {
                String priceText = String.valueOf((long) currentSellPrice);
                java.awt.datatransfer.StringSelection selection =
                        new java.awt.datatransfer.StringSelection(priceText);
                java.awt.datatransfer.Clipboard clipboard =
                        Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, null);
                updateStatus("Sell price copied to clipboard");
            } catch (Exception e) {
                ErrorLogger.logError("Error copying sell price to clipboard", e);
            }
        }
    }

    private void showErrorStatisticsDialog() {
        if (errorLogMonitor == null) {
            JOptionPane.showMessageDialog(this, "Error monitoring is not available.",
                    ERROR_STATISTICS, JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int totalErrors = errorLogMonitor.getTotalErrorCount();
        int sessionErrors = errorLogMonitor.getSessionErrorCount();
        long lastErrorTimeMs = errorLogMonitor.getLastErrorTime();

        StringBuilder message = new StringBuilder();
        message.append("Error Log Statistics\n\n");
        message.append("Total Errors (all time): ").append(totalErrors).append("\n");
        message.append("Session Errors (this run): ").append(sessionErrors).append("\n\n");

        if (lastErrorTimeMs > 0) {
            java.time.LocalDateTime lastErrorTime = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(lastErrorTimeMs),
                    java.time.ZoneId.systemDefault());
            java.time.format.DateTimeFormatter formatter =
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            message.append("Last Error: ").append(lastErrorTime.format(formatter)).append("\n");
        } else {
            message.append("Last Error: None detected\n");
        }

        message.append("\nError log file: error.log");
        message.append("\nMonitoring: ")
                .append(errorLogMonitor.isRunning() ? STATUS_ACTIVE : STATUS_INACTIVE);

        JOptionPane.showMessageDialog(this, message.toString(), ERROR_STATISTICS,
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAboutDialog() {
        String message = APP_NAME + "\n" + "Version " + VERSION + "\n\n"
                + "Analyzes EVE Online Strip Miner rolls by monitoring clipboard.\n"
                + "Supports ORE Strip Miner, Modulated Strip Miner II, and ORE Ice Harvester.\n\n"
                + "Usage:\n" + "1. Select miner type (ORE, Modulated, or Ice)\n"
                + "2. Copy item stats from EVE Online (Ctrl+C on item info)\n"
                + "3. Analysis appears automatically\n\n"
                + "Tier info is automatically copied to clipboard for easy container naming.";

        JOptionPane.showMessageDialog(this, message, MENU_ITEM_ABOUT,
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================================
    // MAIN
    // ============================================================================

    /**
     * Main entry point for the application. Sets up exception handlers and initializes the Swing
     * GUI.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        // Set up global uncaught exception handler for main thread
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> ErrorLogger
                .logError("Uncaught exception in thread: " + thread.getName(), throwable));

        // Set up uncaught exception handler for EDT (Event Dispatch Thread)
        System.setProperty("sun.awt.exception.handler",
                EveMinerAnalyzer.class.getName() + "$EDTExceptionHandler");

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                ErrorLogger.logError("Failed to set system look and feel", e);
                // Use default LAF
            }

            try {
                new EveMinerAnalyzer().setVisible(true);
            } catch (Exception e) {
                ErrorLogger.logError("Fatal error during application startup", e);
                JOptionPane.showMessageDialog(null,
                        "A fatal error occurred during startup. Please check error.log for details.",
                        "Fatal Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /**
     * Handler for uncaught exceptions on the Event Dispatch Thread. This is called via System
     * property sun.awt.exception.handler
     */
    public static class EDTExceptionHandler {
        public void handle(Throwable throwable) {
            ErrorLogger.logError("Uncaught exception on Event Dispatch Thread", throwable);
        }
    }
}

