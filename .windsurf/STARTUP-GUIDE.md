# SAHTY Radiology Application - Guide de Démarrage

## 🚀 Démarrage avec Variables d'Environnement

### Prérequis
- Java 17+
- Maven 3.6+
- Node.js 18+
- PostgreSQL 12+

### Configuration PostgreSQL
Assurez-vous que PostgreSQL est installé et que la base de données `radiology_app` existe.

```sql
CREATE DATABASE radiology_app;
CREATE USER postgres WITH PASSWORD '1234abcD.';
GRANT ALL PRIVILEGES ON DATABASE radiology_app TO postgres;
```

### Méthodes de Démarrage

#### 1. Script Windows (Recommandé)
```bash
.\start-with-env.bat
```

#### 2. Script Linux/macOS
```bash
chmod +x start-with-env.sh
./start-with-env.sh
```

#### 3. Variables d'environnement manuelles
```bash
# Windows
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=radiology_app
set DB_USERNAME=postgres
set DB_PASSWORD=1234abcD.
set PORT=8080
mvn spring-boot:run

# Linux/macOS
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=radiology_app
export DB_USERNAME=postgres
export DB_PASSWORD=1234abcD.
export PORT=8080
mvn spring-boot:run
```

### Fichiers de Configuration

#### application.properties
```properties
server.port=${PORT:8080}
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:radiology_app}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:1234abcD.}
```

#### Variables d'environnement disponibles
| Variable | Valeur par défaut | Description |
|----------|------------------|-------------|
| `DB_HOST` | `localhost` | Hôte PostgreSQL |
| `DB_PORT` | `5432` | Port PostgreSQL |
| `DB_NAME` | `radiology_app` | Nom de la base |
| `DB_USERNAME` | `postgres` | Utilisateur |
| `DB_PASSWORD` | `1234abcD.` | Mot de passe |
| `PORT` | `8080` | Port serveur |

### Accès à l'Application
Une fois démarrée, l'application est accessible à:
- **URL**: http://localhost:8080
- **Interface**: Vaadin 24.3.1

### Résolution des Problèmes

#### Problème npm
```bash
mvn vaadin:clean-frontend
npm install
mvn compile
```

#### Problème de thème
Les fichiers de thème sont déjà configurés dans:
- `frontend/themes/radiology-app/styles.css`
- `frontend/themes/radiology-app/theme.json`

#### Problème Lombok
Les entités utilisent `@Getter @Setter` au lieu de `@Data` pour éviter les problèmes de compilation.

### Structure du Projet
```
radiology-app/
├── src/main/java/          # Code source Java
├── src/main/resources/     # Configuration Spring
├── frontend/               # Frontend Vaadin
│   └── themes/
│       └── radiology-app/
├── scripts/               # Scripts SQL
├── start-with-env.bat     # Script Windows
├── start-with-env.sh      # Script Linux/macOS
└── config.env            # Variables d'environnement
```

### Versions Compatibles
- **Spring Boot**: 3.2.0
- **Java**: 17
- **Vaadin**: 24.3.1
- **PostgreSQL**: 12+
- **Node.js**: 18+
