# Excalibur Gym

Helyi használatra tervezett, böngészőből kezelhető konditermi adminisztrációs rendszer. A projekt a papíralapú vendég-, bérlet- és beléptetési nyilvántartást váltja ki egy gyors recepciós felülettel.

## Fő funkciók

- vendégek és bérlettörténet kezelése
- gyors vendégkeresés és ismételt beléptetés figyelmeztetése
- bérletkiállítás konfigurálható termékekkel és árakkal
- készpénzes és bankkártyás bevételek kimutatása
- napi, heti, havi és éves statisztikák
- adminisztrátori és dolgozói jogosultságok
- szerepkör szerint szűrt, append-only tevékenységnapló
- Liquibase-adatbázismigrációk és opcionális demóadatok

## Technológiák

- Java 25, Spring Boot 4.1, Spring Security, JDBC, Lombok
- Angular 22.1, standalone komponensek, strict TypeScript
- SQLite, Liquibase
- contract-first OpenAPI és generált Spring/Angular API-réteg

## Projektstruktúra

- `backend/` – Spring Boot REST API
- `frontend/` – Angular SPA
- `openapi/` – az API forrásául szolgáló OpenAPI-szerződés

## Helyi indítás

Backend:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Frontend egy másik terminálban:

```powershell
cd frontend
npm install
npm start
```

Az alkalmazás első indításkor a böngészőben kéri az első adminisztrátor létrehozását. A helyi adatbázis nem kerül verziókezelésbe.

## Demóadatok

A demóprofilt kizárólag külön, eldobható adatbázissal használd:

```powershell
cd backend
$env:EXCALIBUR_DB_PATH = "./data/excalibur-demo.db"
mvn spring-boot:run "-Dspring-boot.run.profiles=demo"
```

## Ellenőrzés

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm test -- --watch=false
npm run build
```
