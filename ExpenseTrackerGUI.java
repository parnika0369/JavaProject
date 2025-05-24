import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

// Base Expense class
abstract class Expense {
    private double amount;
    private Date date;
    private String description;

    public Expense(double amount, Date date, String description) {
        this.amount = amount;
        this.date = date;
        this.description = description;
    }

    public double getAmount() { return amount; }
    public Date getDate() { return date; }
    public String getDescription() { return description; }

    public abstract String getType();
}

// FoodExpense class
class FoodExpense extends Expense {
    private String restaurant;

    public FoodExpense(double amount, Date date, String description, String restaurant) {
        super(amount, date, description);
        this.restaurant = restaurant;
    }

    @Override
    public String getType() {
        return "Food";
    }

    public String getRestaurant() { return restaurant; }
}

// TravelExpense class
class TravelExpense extends Expense {
    private String destination;
    private String transportMode;

    public TravelExpense(double amount, Date date, String description, String destination, String transportMode) {
        super(amount, date, description);
        this.destination = destination;
        this.transportMode = transportMode;
    }

    @Override
    public String getType() {
        return "Travel";
    }

    public String getDestination() { return destination; }
    public String getTransportMode() { return transportMode; }
}

// UtilityExpense class
class UtilityExpense extends Expense {
    private String utilityType;

    public UtilityExpense(double amount, Date date, String description, String utilityType) {
        super(amount, date, description);
        this.utilityType = utilityType;
    }

    @Override
    public String getType() {
        return "Utility";
    }

    public String getUtilityType() { return utilityType; }
}

// ExpenseManager class
class ExpenseManager {
    private ArrayList<Expense> expenses;
    private ExpenseDAO expenseDAO;

    public ExpenseManager() {
        this.expenses = new ArrayList<>();
        this.expenseDAO = new ExpenseDAO();
    }

    public void addExpense(Expense exp) {
        expenses.add(exp);
        expenseDAO.insertExpense(exp);
    }

    public ArrayList<Expense> getExpenses() {
        return expenseDAO.getAllExpenses();
    }

    public double getTotalExpenses() {
        double total = 0;
        for (Expense exp : expenses) {
            total += exp.getAmount();
        }
        return total;
    }
}

// ExpenseDAO class for database operations
class ExpenseDAO {
    private Connection connection;

    public ExpenseDAO() {
        connection = DatabaseConnection.getConnection();
    }

    public void insertExpense(Expense exp) {
        String sql = "INSERT INTO expenses (amount, date, description, type, details) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setDouble(1, exp.getAmount());
            pstmt.setDate(2, new java.sql.Date(exp.getDate().getTime()));
            pstmt.setString(3, exp.getDescription());
            pstmt.setString(4, exp.getType());
            if (exp instanceof FoodExpense) {
                pstmt.setString(5, ((FoodExpense) exp).getRestaurant());
            } else if (exp instanceof TravelExpense) {
                pstmt.setString(5, ((TravelExpense) exp).getDestination() + ", " + ((TravelExpense) exp).getTransportMode());
            } else if (exp instanceof UtilityExpense) {
                pstmt.setString(5, ((UtilityExpense) exp).getUtilityType());
            }
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Expense> getAllExpenses() {
        ArrayList<Expense> expenses = new ArrayList<>();
        String sql = "SELECT * FROM expenses";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                double amount = rs.getDouble("amount");
                Date date = rs.getDate("date");
                String description = rs.getString("description");
                String type = rs.getString("type");
                String details = rs.getString("details");

                switch (type) {
                    case "Food":
                        expenses.add(new FoodExpense(amount, date, description, details));
                        break;
                    case "Travel":
                        String[] travelDetails = details.split(", ");
                        expenses.add(new TravelExpense(amount, date, description, travelDetails[0], travelDetails[1]));
                        break;
                    case "Utility":
                        expenses.add(new UtilityExpense(amount, date, description, details));
                        break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return expenses;
    }
}

// DatabaseConnection class for JDBC connection
class DatabaseConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/expense_tracker";
    private static final String USER = "root"; // Change as per your MySQL setup
    private static final String PASSWORD = "password"; // Change as per your MySQL setup

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

// Main GUI class
public class ExpenseTrackerGUI extends JFrame {
    private ExpenseManager manager;
    private JTextField amountField;
    private JTextField descriptionField;
    private JTextField dateField;
    private JComboBox<String> expenseTypeCombo;
    private JTextField restaurantField;
    private JTextField destinationField;
    private JTextField transportField;
    private JTextField utilityTypeField;
    private JButton addExpenseButton;
    private JTable expensesTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;

    public ExpenseTrackerGUI() {
        manager = new ExpenseManager();
        setTitle("Expense Tracker");
        setSize(800, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initComponents();
        layoutComponents();
        registerListeners();
    }

    private void initComponents() {
        amountField = new JTextField(10);
        descriptionField = new JTextField(15);
        dateField = new JTextField(10);
        dateField.setText(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        expenseTypeCombo = new JComboBox<>(new String[]{"Food", "Travel", "Utility"});
        restaurantField = new JTextField(15);
        destinationField = new JTextField(15);
        transportField = new JTextField(15);
        utilityTypeField = new JTextField(15);
        addExpenseButton = new JButton("Add Expense");
        tableModel = new DefaultTableModel(new String[]{"Type", "Amount", "Date", "Description", "Details"}, 0);
        expensesTable = new JTable(tableModel);
        totalLabel = new JLabel("Total Expenses: $0.00");
    }

    private void layoutComponents() {
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Expense Type:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(expenseTypeCombo, gbc);
        gbc.gridx = 2;
        inputPanel.add(new JLabel("Amount:"), gbc);
        gbc.gridx = 3;
        inputPanel.add(amountField, gbc);
        gbc.gridx = 4;
        inputPanel.add(new JLabel("Date (dd-MM-yyyy):"), gbc);
        gbc.gridx = 5;
        inputPanel.add(dateField, gbc);
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        inputPanel.add(descriptionField, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 6;
        inputPanel.add(createDynamicFieldsPanel(), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 6;
        inputPanel.add(addExpenseButton, gbc);
        gbc.gridwidth = 1;

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Expenses"));
        tablePanel.add(new JScrollPane(expensesTable), BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(totalLabel, BorderLayout.WEST);

        Container cp = getContentPane();
        cp.setLayout(new BorderLayout());
        cp.add(inputPanel, BorderLayout.NORTH);
        cp.add(tablePanel, BorderLayout.CENTER);
        cp.add(bottomPanel, BorderLayout.PAGE_END);
    }

    private JPanel createDynamicFieldsPanel() {
        JPanel dynamicPanel = new JPanel(new CardLayout());
        JPanel foodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        foodPanel.add(new JLabel("Restaurant:"));
        foodPanel.add(restaurantField);
        JPanel travelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        travelPanel.add(new JLabel("Destination:"));
        travelPanel.add(destinationField);
        travelPanel.add(new JLabel("Transport Mode:"));
        travelPanel.add(transportField);
        JPanel utilityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        utilityPanel.add(new JLabel("Utility Type:"));
        utilityPanel.add(utilityTypeField);
        dynamicPanel.add(foodPanel, "Food");
        dynamicPanel.add(travelPanel, "Travel");
        dynamicPanel.add(utilityPanel, "Utility");

        CardLayout cl = (CardLayout) dynamicPanel.getLayout();
        cl.show(dynamicPanel, "Food");

        expenseTypeCombo.addActionListener(e -> {
            String selected = (String) expenseTypeCombo.getSelectedItem();
            cl.show(dynamicPanel, selected);
        });

        return dynamicPanel;
    }

    private void registerListeners() {
        addExpenseButton.addActionListener(e -> addExpense());
    }

    private void addExpense() {
        try {
            String type = (String) expenseTypeCombo.getSelectedItem();
            double amount = Double.parseDouble(amountField.getText());
            String desc = descriptionField.getText().trim();
            String dateStr = dateField.getText().trim();
            Date date = new SimpleDateFormat("dd-MM-yyyy").parse(dateStr);
            Expense exp = null;

            switch (type) {
                case "Food":
                    exp = new FoodExpense(amount, date, desc, restaurantField.getText().trim());
                    break;
                case "Travel":
                    exp = new TravelExpense(amount, date, desc, destinationField.getText().trim(), transportField.getText().trim());
                    break;
                case "Utility":
                    exp = new UtilityExpense(amount, date, desc, utilityTypeField.getText().trim());
                    break;
            }

            manager.addExpense(exp);
            refreshTable(manager.getExpenses());
            updateTotalLabel();
            clearInputFields();
            JOptionPane.showMessageDialog(this, "Expense added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshTable(ArrayList<Expense> expenses) {
        tableModel.setRowCount(0);
        for (Expense exp : expenses) {
            String type = exp.getType();
            String amount = String.format("%.2f", exp.getAmount());
            String date = new SimpleDateFormat("dd-MM-yyyy").format(exp.getDate());
            String desc = exp.getDescription();
            String details = "";
            if (exp instanceof FoodExpense) {
                details = ((FoodExpense) exp).getRestaurant();
            } else if (exp instanceof TravelExpense) {
                details = ((TravelExpense) exp).getDestination() + ", " + ((TravelExpense) exp).getTransportMode();
            } else if (exp instanceof UtilityExpense) {
                details = ((UtilityExpense) exp).getUtilityType();
            }
            tableModel.addRow(new Object[]{type, amount, date, desc, details});
        }
    }

    private void updateTotalLabel() {
        totalLabel.setText("Total Expenses: $" + String.format("%.2f", manager.getTotalExpenses()));
    }

    private void clearInputFields() {
        amountField.setText("");
        descriptionField.setText("");
        dateField.setText(new SimpleDateFormat("dd-MM-yyyy").format(new Date()));
        restaurantField.setText("");
        destinationField.setText("");
        transportField.setText("");
        utilityTypeField.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ExpenseTrackerGUI gui = new ExpenseTrackerGUI();
            gui.setVisible(true);
        });
    }
}
