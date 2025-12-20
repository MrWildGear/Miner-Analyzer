package app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import service.ClipboardMonitor;
import ui.AnalysisDisplay;
import ui.ThemeManager;

/**
 * EVE Online Strip Miner Roll Analyzer Reads item stats from clipboard and calculates m3/sec with
 * mutation ranges Supports both ORE Strip Miner and Modulated Strip Miner II
 * 
 */
public class EveMinerAnalyzer extends JFrame {

    private static final String VERSION = "1.2.18";
    private static final String APP_NAME = "EVE Online Strip Miner Roll Analyzer";
    private static final String DEFAULT_STYLE_NAME = "default";

    // UI Components
    private JRadioButton oreRadio;
    private JRadioButton modulatedRadio;
    private JLabel statusLabel;
    private JTextPane resultsText;
    private transient StyledDocument doc;

    private String minerType = "ORE";
    private transient ClipboardMonitor clipboardMonitor;

    // Theme management
    private transient ThemeManager themeManager;
    private transient AnalysisDisplay analysisDisplay;

    // UI Components for theme updates
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel typePanel;
    private JMenuBar appMenuBar;

    public EveMinerAnalyzer() {
        themeManager = new ThemeManager();
        initializeUI();
        startClipboardMonitoring();

        // Clean up on close
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (clipboardMonitor != null) {
                    clipboardMonitor.stop();
                }
                System.exit(0);
            }
        });
    }

    private void initializeUI() {
        setTitle(APP_NAME + " v" + VERSION);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Add menu bar with Theme and Help menus
        appMenuBar = new JMenuBar();

        // Theme menu
        JMenu themeMenu = new JMenu("Theme");

        JMenuItem toggleThemeItem = new JMenuItem("Toggle Theme");
        toggleThemeItem.addActionListener(e -> {
            themeManager.toggleTheme();
            updateUITheme();
        });
        themeMenu.add(toggleThemeItem);

        themeMenu.addSeparator();

        JMenuItem autoThemeItem = new JMenuItem("Auto (Follow System)");
        autoThemeItem.addActionListener(e -> {
            themeManager.setThemeAuto();
            updateUITheme();
        });
        themeMenu.add(autoThemeItem);

        JMenuItem lightThemeItem = new JMenuItem("Light");
        lightThemeItem.addActionListener(e -> {
            themeManager.setThemeLight();
            updateUITheme();
        });
        themeMenu.add(lightThemeItem);

        JMenuItem darkThemeItem = new JMenuItem("Dark");
        darkThemeItem.addActionListener(e -> {
            themeManager.setThemeDark();
            updateUITheme();
        });
        themeMenu.add(darkThemeItem);

        appMenuBar.add(themeMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
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
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
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
        oreRadio = new JRadioButton("ORE", true);
        oreRadio.setBackground(themeManager.getBgColor());
        oreRadio.setForeground(themeManager.getFgColor());
        oreRadio.addActionListener(e -> {
            minerType = "ORE";
            clearResults();
            restartClipboardMonitoring();
        });
        typeGroup.add(oreRadio);
        typePanel.add(oreRadio);

        modulatedRadio = new JRadioButton("Modulated");
        modulatedRadio.setBackground(themeManager.getBgColor());
        modulatedRadio.setForeground(themeManager.getFgColor());
        modulatedRadio.addActionListener(e -> {
            minerType = "Modulated";
            clearResults();
            restartClipboardMonitoring();
        });
        typeGroup.add(modulatedRadio);
        typePanel.add(modulatedRadio);

        headerPanel.add(typePanel, BorderLayout.CENTER);

        // Status label
        statusLabel = new JLabel("Monitoring clipboard...");
        statusLabel.setForeground(themeManager.getFgColor());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        headerPanel.add(statusLabel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Results panel
        resultsText = new JTextPane();
        resultsText.setEditable(false);
        resultsText.setBackground(themeManager.getFrameBg());
        resultsText.setForeground(themeManager.getFgColor());
        resultsText.setFont(new Font("Consolas", Font.PLAIN, 10));
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
        } catch (Exception ignored) {
            // Ignore style setup errors
        }

        // Initialize analysis display
        analysisDisplay =
                new AnalysisDisplay(doc, themeManager.getFgColor(), themeManager.getTierColors());

        JScrollPane scrollPane = new JScrollPane(resultsText);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        scrollPane.getViewport().setBackground(themeManager.getFrameBg());
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);

        // Initial message
        analysisDisplay.appendText("Miner type set to: ORE\n", themeManager.getFgColor());
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
        } catch (Exception ignored) {
            // Ignore style errors
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
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timeStr = java.time.LocalTime.now().toString();
                String timestamp = timeStr.length() >= 8 ? timeStr.substring(0, 8) : timeStr;
                statusLabel.setText(message + " - " + timestamp);
            } catch (Exception ignored) {
                // Fallback if timestamp formatting fails
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

    private void showAboutDialog() {
        String message = APP_NAME + "\n" + "Version " + VERSION + "\n\n"
                + "Analyzes EVE Online Strip Miner rolls by monitoring clipboard.\n"
                + "Supports ORE Strip Miner and Modulated Strip Miner II.\n\n" + "Usage:\n"
                + "1. Select miner type (ORE or Modulated)\n"
                + "2. Copy item stats from EVE Online (Ctrl+C on item info)\n"
                + "3. Analysis appears automatically\n\n"
                + "Tier info is automatically copied to clipboard for easy container naming.";

        JOptionPane.showMessageDialog(this, message, "About", JOptionPane.INFORMATION_MESSAGE);
    }

    // ============================================================================
    // MAIN
    // ============================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use default LAF
            }

            new EveMinerAnalyzer().setVisible(true);
        });
    }
}

