# 🎥Fidelio

[![GitHub repo size](https://img.shields.io/github/repo-size/Lucas5N/fidelio)]()
[![GitHub language count](https://img.shields.io/github/languages/count/Lucas5N/fidelio)]()
[![Top language](https://img.shields.io/github/languages/top/Lucas5N/fidelio)]()

**Fidelio** è una piattaforma di social media, sviluppata per l'esame di Ingegneria del Software, progettata specificamente per gli appassionati di cinema. Offre uno spazio dedicato in cui gli utenti possono esplorare film, condividere opinioni e avviare discussioni approfondite sulla settima arte.

## Indice

1. [Panoramica del Progetto](#panoramica-del-progetto)
2. [Stack Tecnologico](#stack-tecnologico)
3. [Documentazione](#documentazione)
4. [Installazione e Setup](#installazione-e-setup)

---

## Panoramica del Progetto

L'obiettivo di Fidelio è unire le dinamiche tipiche dei social network con un focus tematico sul cinema. Gli utenti possono interagire tramite un'interfaccia web responsiva e dinamica, discutere di pellicole, scambiarsi recensioni e creare una community dedicata al mondo cinematografico.

## Stack Tecnologico

Il progetto è sviluppato seguendo un'architettura robusta e containerizzata:

* **Backend:** Java con SpringBoot
* **Frontend:** HTML5, CSS3 e Vanilla JavaScript
* **Build Tool:** Maven (pom.xml, wrapper inclusi)
* **Containerizzazione:** Docker (Dockerfile incluso)

## Documentazione

L'architettura del software, i diagrammi di sistema e le specifiche funzionali sono descritte nel dettaglio nei documenti allegati alla repository.

Per comprendere a fondo le logiche di progettazione, le scelte architetturali e i flussi di dati, si prega di fare riferimento alla cartella dedicata:
👉 **[Consulta la Documentazione completa qui](./Documentazione)**

## Installazione e Setup

Il processo di deployment e configurazione del progetto è documentato separatamente per garantire la massima chiarezza. All'interno della directory dedicata all'installazione troverai tutti i passaggi per configurare il database, le variabili d'ambiente e l'infrastruttura necessaria.

👉 **[Consulta la Guida all'Installazione qui](./Installation)**

### Esecuzione Rapida con Docker
Essendo il progetto dotato di un `Dockerfile`, è possibile effettuare una build veloce e avviare il container tramite Docker. (Fai sempre riferimento ai documenti nella cartella `Installation` per i prerequisiti completi).

```bash
# Esempio di build dell'immagine Docker
docker build -t fidelio-app .

# Esempio di esecuzione del container
docker run -p 8080:8080 fidelio-app
