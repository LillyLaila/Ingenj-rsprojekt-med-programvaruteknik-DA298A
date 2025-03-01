# Ingenjörsprojekt-med-programvaruteknik-DA298A
I denna kurs ska man utveckla och programmera ett inbyggt system som del av en produkt och ska konstrueras i projektgrupper.

## Projektöversikt
Detta projekt är ett distriburerat system för att visualisera och hantera en brandsituation. Systemet består av en server, en gateway och en slav (ESP32 med TFT-skärm). Servern genererar en karta med rök och eld, som sedan skickas via en gateway till en slav-enhet. Brandmän kan interagera med systemet genom att skicka åtgärder tillbaka till servern.

## Kommunikation mellan hårdvara och mjukvara
Systemet använder en kombination av seriell kommunikation och ESP-NOW för att överföra data mellan enheterna.
1. Seriell kommunikation (Server <-> Gateway)   
2. ESP-NOW (Gateway <-> Slav)

## Hårdvara
- ESP32 (Gateway & slav) 
- TFT-skärm (ILI9341, 240x320) - Används på slav-enheten för att visa kartan 
- Dator (Server) - Kör Java-servern och hanterar kartgenerering. 
