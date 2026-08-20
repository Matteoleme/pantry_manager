# Miglioramento Filtraggio Prodotti e Gestione Esauriti

Questo piano descrive le modifiche necessarie per rendere i filtri per categoria sempre visibili e implementare la logica per i prodotti esauriti ("Finiti").

## User Review Required

> [!IMPORTANT]
> La logica di filtraggio è stata modificata per escludere di default i prodotti con quantità <= 0 dalla vista "Tutti" e dalle singole categorie. Saranno visibili solo selezionando il nuovo filtro "Finiti".

## Proposed Changes

### UI & ViewModel

#### [MODIFY] [PantryViewModel.kt](file:///D:/MACC/pantry_manager/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/PantryViewModel.kt)
- Aggiunta di `showOnlyOutOfStock` al `PantryUiState`.
- Aggiornamento della property calcolata `filteredProducts` per gestire l'esclusione/inclusione dei prodotti esauriti.
- Aggiunta della funzione `toggleOutOfStockFilter()` per gestire il nuovo chip.
- Aggiornamento di `toggleCategory` e `clearCategoryFilters` per resettare reciprocamente i filtri (se seleziono "Finiti", tolgo le categorie e viceversa).

#### [MODIFY] [HomeScreen.kt](file:///D:/MACC/pantry_manager/Xpensa/app/src/main/java/com/mobileapp/xpensa/ui/home/HomeScreen.kt)
- Rimozione del flag `showCategoryFilter` (o impostazione a `true` di default) per rendere i filtri sempre visibili.
- Aggiornamento di `CategoryFilterRow` per includere il chip "Finiti" (o "Esauriti").
- Passaggio dei nuovi parametri e callback dal ViewModel alla UI.

## Verification Plan

### Manual Verification
1. **Vista "Tutti"**: Verificare che i prodotti con quantità > 0 siano visibili e quelli con quantità <= 0 siano nascosti.
2. **Filtro Categoria**: Verificare che selezionando una categoria (es. "Verdure") siano visibili solo le verdure con quantità > 0.
3. **Filtro "Finiti"**: Verificare che selezionando "Finiti" vengano mostrati TUTTI i prodotti con quantità <= 0, indipendentemente dalla categoria.
4. **Ricerca**: Verificare che la barra di ricerca funzioni correttamente in combinazione con i filtri (es. cercare "Mele" tra i "Finiti").
5. **Visibilità**: Confermare che la riga dei filtri sia visibile all'apertura della HomeScreen senza azioni aggiuntive.
