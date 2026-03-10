# Hallenbelegungsplan

Webbasierte Anwendung zur Verwaltung der Hallenbelegung einer Gemeinde.

Das System ersetzt statische Belegungspläne (z. B. PDFs) durch einen dynamischen Online-Kalender, über den Vereine Trainingszeiten verwalten und die Gemeindeverwaltung Buchungen freigeben kann.

---

## Features

- Wochenkalender für Hallenbelegung
- Buchungsanfragen für Vereine
- Freigabe durch Verwaltung
- Sperrzeiten und Konfliktprüfung
- statistische Auswertung der Nutzung

---

## Architektur (geplant)

Frontend  
React (SPA)

Backend  
REST API (Quarkus)

Datenbank  
PostgreSQL

Deployment  
Docker + Reverse Proxy + HTTPS

---

## Security Fokus

Das Projekt wird bewusst mit Fokus auf **Web-Security** entwickelt.

Geplante Aspekte:

- sichere Authentifizierung
- rollenbasierte Zugriffskontrolle
- serverseitige Validierung
- sichere Passwortspeicherung
- Analyse möglicher Web-Vulnerabilities (z. B. Access Control, Injection)

---

## Dokumentation

Lastenheft  
docs/lastenheft/lastenheft.pdf

Technisches Pflichtenheft folgt.

---

## Status

Projektphase: Planung  
Next step: Pflichtenheft und Architektur.
