# Package-Tracking-System
A full-featured package tracking system built with SQL, designed to manage senders, receivers, packages, couriers, and real-time status updates.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Database Schema](#database-schema)
- [SQL Tables](#sql-tables)
- [Sample Queries](#sample-queries)
- [Setup Instructions](#setup-instructions)
- [ER Diagram](#er-diagram)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**PackageTrack** is a relational database-driven system for managing end-to-end package delivery. It tracks senders and receivers, assigns courier agents, logs real-time package status, and generates delivery reports — all powered by a normalized SQL schema.

This project is ideal for:
- Logistics companies managing shipments
- E-commerce platforms needing order tracking
- Learning relational database design with real-world use cases

---

## Features

- Register **senders** and **receivers** with full contact details
- Create and assign **packages** with weight, dimensions, and type
- Assign **courier agents** to delivery routes
- Track **package status** at every stage (Picked Up → In Transit → Delivered)
- Record **delivery locations** and timestamps
- Generate tracking history per package

---

## Database Schema

The system uses **6 core tables** with proper foreign key relationships:

```
sender ──< package >── receiver
                │
              courier
                │
          package_status
                │
            location
```

---

## SQL Tables

### 1. `sender`
Stores information about individuals or businesses sending packages.

```sql
CREATE TABLE sender (
    sender_id     INT PRIMARY KEY AUTO_INCREMENT,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  UNIQUE NOT NULL,
    phone         VARCHAR(20),
    address_line1 VARCHAR(255)  NOT NULL,
    address_line2 VARCHAR(255),
    city          VARCHAR(100)  NOT NULL,
    state         VARCHAR(100),
    postal_code   VARCHAR(20)   NOT NULL,
    country       VARCHAR(100)  NOT NULL DEFAULT 'India',
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
```

---

### 2. `receiver`
Stores information about the recipient of a package.

```sql
CREATE TABLE receiver (
    receiver_id   INT PRIMARY KEY AUTO_INCREMENT,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(150),
    phone         VARCHAR(20)   NOT NULL,
    address_line1 VARCHAR(255)  NOT NULL,
    address_line2 VARCHAR(255),
    city          VARCHAR(100)  NOT NULL,
    state         VARCHAR(100),
    postal_code   VARCHAR(20)   NOT NULL,
    country       VARCHAR(100)  NOT NULL DEFAULT 'India',
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
```

---

### 3. `courier`
Stores delivery agent details.

```sql
CREATE TABLE courier (
    courier_id    INT PRIMARY KEY AUTO_INCREMENT,
    full_name     VARCHAR(100)  NOT NULL,
    phone         VARCHAR(20)   NOT NULL,
    email         VARCHAR(150)  UNIQUE,
    vehicle_type  ENUM('Bike', 'Van', 'Truck', 'Drone') DEFAULT 'Bike',
    is_available  BOOLEAN       DEFAULT TRUE,
    rating        DECIMAL(3,2)  DEFAULT 5.00,
    created_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);
```

---

### 4. `package`
The central table linking senders, receivers, and couriers.

```sql
CREATE TABLE package (
    package_id        INT PRIMARY KEY AUTO_INCREMENT,
    tracking_number   VARCHAR(20)   UNIQUE NOT NULL,
    sender_id         INT           NOT NULL,
    receiver_id       INT           NOT NULL,
    courier_id        INT,
    weight_kg         DECIMAL(6,2)  NOT NULL,
    length_cm         DECIMAL(6,2),
    width_cm          DECIMAL(6,2),
    height_cm         DECIMAL(6,2),
    package_type      ENUM('Document', 'Parcel', 'Fragile', 'Perishable', 'Electronics') DEFAULT 'Parcel',
    declared_value    DECIMAL(10,2),
    shipping_cost     DECIMAL(8,2)  NOT NULL,
    is_insured        BOOLEAN       DEFAULT FALSE,
    estimated_delivery DATE,
    created_at        TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (sender_id)   REFERENCES sender(sender_id),
    FOREIGN KEY (receiver_id) REFERENCES receiver(receiver_id),
    FOREIGN KEY (courier_id)  REFERENCES courier(courier_id)
);
```

---

### 5. `location`
Stores waypoints and depot locations used during transit.

```sql
CREATE TABLE location (
    location_id   INT PRIMARY KEY AUTO_INCREMENT,
    location_name VARCHAR(150)  NOT NULL,
    city          VARCHAR(100)  NOT NULL,
    state         VARCHAR(100),
    country       VARCHAR(100)  NOT NULL DEFAULT 'India',
    postal_code   VARCHAR(20),
    latitude      DECIMAL(9,6),
    longitude     DECIMAL(9,6),
    location_type ENUM('Warehouse', 'Hub', 'Delivery Point', 'Pickup Point') DEFAULT 'Hub'
);
```

---

### 6. `package_status`
Logs every status change for a package — the tracking history.

```sql
CREATE TABLE package_status (
    status_id     INT PRIMARY KEY AUTO_INCREMENT,
    package_id    INT           NOT NULL,
    location_id   INT,
    status        ENUM(
                    'Order Placed',
                    'Picked Up',
                    'In Transit',
                    'At Hub',
                    'Out for Delivery',
                    'Delivered',
                    'Failed Delivery',
                    'Returned'
                  ) NOT NULL,
    remarks       VARCHAR(255),
    updated_at    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (package_id)  REFERENCES package(package_id),
    FOREIGN KEY (location_id) REFERENCES location(location_id)
);
```

---

## Sample Queries

### Track a package by tracking number
```sql
SELECT
    p.tracking_number,
    s.full_name      AS sender,
    r.full_name      AS receiver,
    r.city           AS destination,
    ps.status,
    l.location_name  AS current_location,
    ps.updated_at
FROM package p
JOIN sender          s  ON p.sender_id   = s.sender_id
JOIN receiver        r  ON p.receiver_id = r.receiver_id
JOIN package_status  ps ON p.package_id  = ps.package_id
LEFT JOIN location   l  ON ps.location_id = l.location_id
WHERE p.tracking_number = 'PKT-20240001'
ORDER BY ps.updated_at DESC;
```

---

### Get all packages assigned to a courier
```sql
SELECT
    p.tracking_number,
    s.full_name  AS sender,
    r.full_name  AS receiver,
    r.city       AS delivery_city,
    p.estimated_delivery,
    ps.status
FROM package p
JOIN sender         s  ON p.sender_id   = s.sender_id
JOIN receiver       r  ON p.receiver_id = r.receiver_id
JOIN package_status ps ON p.package_id  = ps.package_id
WHERE p.courier_id = 3
  AND ps.updated_at = (
      SELECT MAX(updated_at)
      FROM package_status
      WHERE package_id = p.package_id
  );
```

---

### Count packages by current status
```sql
SELECT
    ps.status,
    COUNT(*) AS total_packages
FROM package_status ps
INNER JOIN (
    SELECT package_id, MAX(updated_at) AS latest
    FROM package_status
    GROUP BY package_id
) latest_ps ON ps.package_id = latest_ps.package_id
           AND ps.updated_at  = latest_ps.latest
GROUP BY ps.status
ORDER BY total_packages DESC;
```

---

### Insert a new package with status
```sql
-- Step 1: Add sender
INSERT INTO sender (full_name, email, phone, address_line1, city, postal_code, country)
VALUES ('Rahul Sharma', 'rahul@example.com', '9876543210', '12 MG Road', 'Delhi', '110001', 'India');

-- Step 2: Add receiver
INSERT INTO receiver (full_name, phone, address_line1, city, postal_code, country)
VALUES ('Priya Mehta', '9123456780', '45 Park Street', 'Mumbai', '400001', 'India');

-- Step 3: Create package
INSERT INTO package (tracking_number, sender_id, receiver_id, weight_kg, package_type, shipping_cost, estimated_delivery)
VALUES ('PKT-20240001', 1, 1, 2.50, 'Parcel', 150.00, '2024-08-10');

-- Step 4: Log initial status
INSERT INTO package_status (package_id, status, remarks)
VALUES (1, 'Order Placed', 'Package registered at origin facility');
```

---

## Setup Instructions

### Prerequisites
- MySQL 8.0+ or MariaDB 10.5+
- MySQL Workbench / DBeaver / any SQL client

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/your-username/package-tracking-system.git
cd package-tracking-system

# 2. Open your SQL client and run the schema
mysql -u root -p < schema/create_tables.sql

# 3. (Optional) Load sample data
mysql -u root -p packagetrack < schema/sample_data.sql

# 4. Verify tables
mysql -u root -p -e "USE packagetrack; SHOW TABLES;"
```

---

## ER Diagram

```
┌───────────┐       ┌─────────────┐       ┌──────────────┐
│  sender   │       │   package   │       │   receiver   │
│───────────│       │─────────────│       │──────────────│
│ sender_id │──────<│ sender_id   │>──────│ receiver_id  │
│ full_name │       │ receiver_id │       │ full_name    │
│ email     │       │ courier_id  │       │ phone        │
│ phone     │       │ tracking_no │       │ address      │
│ address   │       │ weight_kg   │       │ city         │
└───────────┘       │ shipping_$  │       └──────────────┘
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │                         │
    ┌─────────▼──────┐       ┌──────────▼──────┐
    │    courier     │       │ package_status  │
    │────────────────│       │─────────────────│
    │ courier_id     │       │ status_id        │
    │ full_name      │       │ package_id       │
    │ vehicle_type   │       │ location_id      │
    │ is_available   │       │ status           │
    └────────────────┘       │ updated_at       │
                             └────────┬─────────┘
                                      │
                             ┌────────▼─────────┐
                             │    location      │
                             │──────────────────│
                             │ location_id      │
                             │ location_name    │
                             │ city, state      │
                             │ latitude/long    │
                             └──────────────────┘
```

---

## Project Structure

```
package-tracking-system/
├── schema/
│   ├── create_tables.sql      # All CREATE TABLE statements
│   ├── sample_data.sql        # Seed data for testing
│   └── stored_procedures.sql  # Useful stored procedures
├── queries/
│   ├── tracking_queries.sql   # Package tracking queries
│   ├── reports.sql            # Business report queries
│   └── admin_queries.sql      # Admin/ops queries
├── docs/
│   └── ER_diagram.png         # Visual ER diagram
└── README.md
```

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/add-notifications`
3. Commit your changes: `git commit -m "Add SMS notification table"`
4. Push to the branch: `git push origin feature/add-notifications`
5. Open a Pull Request

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

> Built with SQL · Designed for Logistics · Open Source
