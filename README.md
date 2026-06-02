# Backend – teknologivalg og begrunnelser

Denne backend-løsningen er laget som en del av fagprøven i IT-utviklerfaget. Formålet med løsningen er å vise forståelse for valg av teknologi, arkitektur og struktur, samt evne til å begrunne disse valgene på en profesjonell måte.

---

## Valg av rammeverk – Spring Boot

Backend er utviklet med **Spring Boot**, som er et veletablert og mye brukt rammeverk for utvikling av backend-applikasjoner i Java-økosystemet. Spring Boot er valgt fordi det:

- Gir rask oppstart med gode standardkonfigurasjoner
- Forenkler utvikling av REST-baserte API-er
- Har tydelige konvensjoner og struktur
- Er mye brukt i profesjonelle utviklingsmiljøer
- Har tidligere erfaring med utvikling i spring boot

Ved å bruke Spring Boot reduseres behovet for manuell oppsett og konfigurasjon, slik at fokuset kan ligge på funksjonalitet, kvalitet og god kodepraksis.

---

## Valg av programmeringsspråk – Kotlin

Backend er skrevet i **Kotlin**, som er fullt kompatibelt med Java og støttes godt av Spring Boot. Kotlin er valgt fordi språket:

- Gir mer konsis og lesbar kode enn Java
- Har innebygd null-sikkerhet, som reduserer risiko for feil
- Krever mindre boilerplate-kode
- Har tidligere erfaring med utvikling i kotlin

Dette bidrar til bedre kodekvalitet og enklere vedlikehold over tid.

---

## Arkitektur – REST-basert API

Applikasjonen er bygget som et **REST-API**, der funksjonalitet eksponeres via HTTP-endepunkter og JSON-responser. Dette gir:

- Et tydelig skille mellom frontend og backend
- Mulighet for gjenbruk av backend mot flere klienter
- Enkel integrasjon med frontend-løsninger som React og Next.js
