# ParkourTimer

A simple plugin that provides a parkour timer with action bar updates for Paper-based lobby servers.

---

## Features
- Start/stop timers when stepping on pressure plates
- Action bar timer display
- Restart/cancel timer using inventory control items
- Timeout handling if maximum parkour time is exceeded

---

## Configuration (`config.yml`)
- Reset location for players
- Maximum parkour time
- Messages for start, complete, reset, cancel, timeout
- Action bar format
- Sounds for start and completion

*(Default `config.yml` is generated on first run.)*

## MySQL Setup with XAMPP

If you want ParkourTimer to store data in a MySQL database, you can use **XAMPP** to set it up locally:

### 1. Start XAMPP
1. Open the **XAMPP Control Panel**.
2. Start **Apache** and **MySQL**.

### 2. Open phpMyAdmin
1. Click **Admin** next to MySQL in the XAMPP Control Panel.
2. This opens phpMyAdmin in your browser.

### 3. Create a User
1. Go to **User Accounts** and click **Add user account**.
2. Enter:
    - **Username:** `parkour`
    - **Password:** `password`
3. Click **Create database with same name and grant all privileges**.
4. Click **Go**.

### 5. Configure the Plugin
Edit `config.yml`:

```yaml
database:
  host: localhost
  port: 3306
  name: parkour
  user: parkour
  password: password
```

---

## Version
- **Current Version:** 1.1.0  
- **API Version:** 1.20  

## Planned Features
- Leaderboards
- Challenge Mode
- Multiple Configurable Courses

---

## Author
`MrTytanic` - Developer of ParkourTimer
