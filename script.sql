-- Create the database
CREATE DATABASE expense_tracker;

-- Use the database
USE expense_tracker;

-- Create Users table
CREATE TABLE Users (
    UserID SERIAL PRIMARY KEY,
    Username VARCHAR(50) NOT NULL,
    Email VARCHAR(100) NOT NULL UNIQUE,
    Password VARCHAR(255) NOT NULL
);

-- Create Accounts table
CREATE TABLE Accounts (
    AccountID SERIAL PRIMARY KEY,
    UserID INT REFERENCES Users(UserID),
    AccountName VARCHAR(50) NOT NULL,
    Balance DECIMAL(10, 2) NOT NULL
);

-- Create Categories table
CREATE TABLE Categories (
    CategoryID SERIAL PRIMARY KEY,
    UserID INT REFERENCES Users(UserID),
    CategoryName VARCHAR(50) NOT NULL
);

-- Create Transactions table
CREATE TABLE Transactions (
    TransactionID SERIAL PRIMARY KEY,
    UserID INT REFERENCES Users(UserID),
    AccountID INT REFERENCES Accounts(AccountID),
    CategoryID INT REFERENCES Categories(CategoryID),
    Amount DECIMAL(10, 2) NOT NULL,
    TransactionDate DATE NOT NULL,
    Description VARCHAR(255),
    Type VARCHAR(10) CHECK (Type IN ('Income', 'Expense'))
);

-- Insert sample data into Users table
INSERT INTO Users (Username, Email, Password) VALUES 
('user1', 'user1@example.com', 'password1'),
('user2', 'user2@example.com', 'password2');

-- Insert sample data into Accounts table
INSERT INTO Accounts (User ID, AccountName, Balance) VALUES 
(1, 'Savings Account', 5000.00),
(1, 'Checking Account', 1500.00),
(2, 'Business Account', 3000.00);

-- Insert sample data into Categories table
INSERT INTO Categories (User ID, CategoryName) VALUES 
(1, 'Groceries'),
(1, 'Utilities'),
(2, 'Travel'),
(2, 'Entertainment');

-- Insert sample data into Transactions table
INSERT INTO Transactions (User ID, AccountID, CategoryID, Amount, TransactionDate, Description, Type) VALUES 
(1, 1, 1, 50.00, '2024-03-26', 'Grocery shopping', 'Expense'),
(1, 1, 2, 100.00, '2024-03-25', 'Electricity bill', 'Expense'),
(2, 3, 3, 200.00, '2024-03-20', 'Flight to New York', 'Expense'),
(2, 3, 4, 150.00, '2024-03-21', 'Concert tickets', 'Expense');

-- Query to retrieve all transactions
SELECT * FROM Transactions;

-- Query to calculate total expenses for a specific category
SELECT Categories.CategoryName, SUM(Transactions.Amount) AS TotalExpenses  
FROM Transactions  
JOIN Categories ON Transactions.CategoryID = Categories.CategoryID  
WHERE Transactions.UserID = 1 AND Transactions.Type = 'Expense'  
GROUP BY Categories.CategoryName;

-- Query to retrieve account balances for all accounts
SELECT AccountName, Balance FROM Accounts;