# Secure Exam System

This project is a multi-tier application consisting of a Java Spring Boot backend, a Vue.js frontend, and a MySQL database, all containerized with Docker.

## Project Structure

- `repo/`: Contains all source code.
  - `repo/src/`: Backend Java/Maven source code.
  - `repo/frontend/`: Frontend Vue.js application.
  - `repo/Dockerfile`: Docker configuration for the backend.
  - `repo/docker-compose.yml`: Main Docker orchestration file.
  - `repo/frontend/Dockerfile`: Docker configuration for the frontend.

## Prerequisites

- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Getting Started

To build and start the entire system, navigate to the `repo/` directory and run:

```bash
cd repo
docker-compose up --build
```

This will:
1.  Start the **MySQL** database container (`exam_system`).
2.  Build and start the **Backend** service on `http://localhost:8080`.
3.  Build and start the **Frontend** service on `http://localhost:5173`.

### Accessing the Application

- **Frontend**: [http://localhost:5173](http://localhost:5173)
- **Backend API**: [http://localhost:8080](http://localhost:8080)
- **MySQL Database**: `localhost:3306` (Internal: `mysql:3306`)

## Configuration

The following environment variables are set in `docker-compose.yml`:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `MYSQL_ROOT_PASSWORD` | `root` | MySQL root password |
| `MYSQL_DATABASE` | `exam_system` | Target database name |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://mysql:3306/exam_system` | Backend database connection string |
| `APP_SECURITY_AES_BASE64_KEY` | `MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=` | Base64 AES Key |

## Development

### Backend Commands (Requires Java & Maven)
```bash
cd repo
mvn clean package
```

### Frontend Commands (Requires Node.js)
```bash
cd repo/frontend
npm install
npm run dev
```

## Global Gitignore

A global `.gitignore` is provided in the project root to exclude common build artifacts, IDE configurations, and environment-specific files.
