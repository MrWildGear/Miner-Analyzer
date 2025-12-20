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

    private static final String VERSION = "1.2.7";
    private static final String APP_NAME = "EVE Online Strip Miner Roll Analyzer";

    // UI Components
    private JRadioButton oreRadio;
    private JRadioButton modulatedRadio;
    private JLabel statusLabel;
    private JTextPane resultsText;
    private StyledDocument doc;

    private String minerType = "ORE";
    private ClipboardMonitor clipboardMonitor;

    // Theme management
    private ThemeManager themeManager;
    private AnalysisDisplay analysisDisplay;

    // UI Components for theme updates
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel typePanel;
    private JMenuBar menuBar;

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
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        // Add menu bar with Theme and Help menus
        menuBar = new JMenuBar();

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

        menuBar.add(themeMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        // Style all menu items initially
        updateMenuBarColors(menuBar);

        setJMenuBar(menuBar);
        menuBar.setBackground(themeManager.getMenuBgColor());
        menuBar.setForeground(themeManager.getMenuFgColor());

        // Main panel
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(themeManager.getBgColor());

        // Header panel
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(themeManager.getHeaderBgColor());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLabel = new JLabel("EVE Online Strip Miner Roll Analyzer");
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
        doc = resultsText.getStyledDocument();

        // Set up default style for the document to ensure correct text color
        try {
            javax.swing.text.Style defaultStyle = doc.getStyle("default");
            if (defaultStyle == null) {
                defaultStyle = doc.addStyle("default", null);
            }
            if (defaultStyle != null) {
                javax.swing.text.StyleConstants.setForeground(defaultStyle,
                        themeManager.getFgColor());
                doc.setLogicalStyle(0, defaultStyle);
            }
        } catch (Exception e) {
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
            // Update menu bar
            if (menuBar != null) {
                menuBar.setBackground(themeManager.getMenuBgColor());
                menuBar.setForeground(themeManager.getMenuFgColor());
                updateMenuBarColors(menuBar);
            }

            // Update main panel
            if (mainPanel != null) {
                mainPanel.setBackground(themeManager.getBgColor());
            }

            // Update header panel
            if (headerPanel != null) {
                headerPanel.setBackground(themeManager.getHeaderBgColor());
                headerPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            }

            // Update type panel
            if (typePanel != null) {
                typePanel.setBackground(themeManager.getBgColor());
            }

            // Update labels and nested components
            updateComponentTheme(getContentPane());

            // Update radio buttons
            if (oreRadio != null) {
                oreRadio.setBackground(themeManager.getBgColor());
                oreRadio.setForeground(themeManager.getFgColor());
            }
            if (modulatedRadio != null) {
                modulatedRadio.setBackground(themeManager.getBgColor());
                modulatedRadio.setForeground(themeManager.getFgColor());
            }

            // Update status label
            if (statusLabel != null) {
                statusLabel.setForeground(themeManager.getFgColor());
            }

            // Update text pane
            if (resultsText != null) {
                resultsText.setBackground(themeManager.getFrameBg());
                resultsText.setForeground(themeManager.getFgColor());
                // Set logical style to ensure default text color is correct
                try {
                    javax.swing.text.Style style =
                            resultsText.getStyledDocument().getStyle("default");
                    if (style == null) {
                        style = resultsText.getStyledDocument().addStyle("default", null);
                    }
                    if (style != null) {
                        javax.swing.text.StyleConstants.setForeground(style,
                                themeManager.getFgColor());
                        resultsText.getStyledDocument().setLogicalStyle(0, style);
                    }
                } catch (Exception e) {
                    // Ignore style errors
                }
            }

            // Update scroll pane border
            Component[] components = mainPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JScrollPane) {
                    JScrollPane scrollPane = (JScrollPane) comp;
                    scrollPane.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(themeManager.getBorderColor(), 1),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
                    scrollPane.getViewport().setBackground(themeManager.getFrameBg());
                }
            }

            // Update analysis display theme
            if (analysisDisplay != null) {
                analysisDisplay.updateTheme(themeManager.getFgColor(),
                        themeManager.getTierColors());
            }

            // Repaint
            repaint();
        });
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
        if (item instanceof JMenu) {
            JMenu submenu = (JMenu) item;
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
                if (comp instanceof JMenuItem) {
                    styleMenuItem((JMenuItem) comp);
                }
            }
        }
    }

    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                JLabel label = (JLabel) comp;
                if (label != statusLabel) { // statusLabel is handled separately
                    label.setForeground(themeManager.getFgColor());
                }
            } else if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                panel.setBackground(themeManager.getBgColor());
                updateComponentTheme(panel);
            } else if (comp instanceof JScrollPane) {
                updateComponentTheme((Container) comp);
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
            } catch (Exception e) {
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
            } catch (Exception e) {
                // Use default LAF
            }

            new EveMinerAnalyzer().setVisible(true);
        });
    }
}
