# Workflow des Examens Temporaires - Modal de Création d'Ordre

## 🎯 Objectif

Permettre à l'utilisateur d'ajouter des examens dans le modal sans les sauvegarder immédiatement en base de données. Les examens ne sont sauvegardés que lors de la validation finale de l'ordre.

## 🔄 Workflow Modifié

### 1. **Phase de Saisie**
- **Patient & Médecin** : Sélection obligatoire avant d'ajouter des examens
- **Hôpital** : Automatiquement assigné selon l'utilisateur connecté

### 2. **Phase d'Ajout d'Examens**
- **Ajout Temporaire** : Les examens sont stockés dans une liste `tempExams` en mémoire
- **Pas de Sauvegarde BDD** : Ni les procédures ni les examens ne sont sauvegardés à ce stade
- **Mise à Jour UI** : Le tableau et le compteur s'actualisent immédiatement

### 3. **Phase de Sauvegarde Finale**
- **Transaction Unique** : Tout est sauvegardé en une seule transaction
- **Ordre d'Opérations** :
  1. Sauvegarde de l'ordre
  2. Sauvegarde des procédures (si non existantes)
  3. Sauvegarde des examens avec liaison à l'ordre
  4. Mise à jour de l'ordre avec les examens

## 🔧 Modifications Techniques

### Liste Temporaire
```java
// Liste temporaire pour stocker les examens avant sauvegarde
List<Exam> tempExams = new ArrayList<>();
if (isEdit && order != null && order.getExams() != null) {
    tempExams.addAll(order.getExams());
}
```

### Ajout d'Examen (Mémoire)
```java
// Ajouter à la liste temporaire (pas à la BDD)
tempExams.add(newExam);

// Refresh grid and update count
examsGrid.setItems(tempExams);
```

### Sauvegarde Finale (Transaction)
```java
for (Exam exam : exams) {
    // First save the procedure if it's not already saved
    if (exam.getProcedure() != null && exam.getProcedure().getId() == null) {
        Procedure savedProcedure = procedureRepository.save(exam.getProcedure());
        exam.setProcedure(savedProcedure);
    }
    
    // Set order relationship and save exam
    exam.setOrder(order);
    examRepository.save(exam);
    order.addExam(exam);
}
```

## ✅ Avantages

### 1. **Performance**
- **Une Seule Transaction** : Évite les multiples allers-retours BDD
- **Validation Groupée** : Tous les examens sont validés en une fois

### 2. **Expérience Utilisateur**
- **Annulation Possible** : L'utilisateur peut fermer le modal sans impacter la BDD
- **Modification Facile** : Les examens peuvent être modifiés/supprimés avant sauvegarde
- **Feedback Immédiat** : Le tableau s'actualise instantanément

### 3. **Intégrité des Données**
- **Tout ou Rien** : Soit tout l'ordre avec ses examens est sauvegardé, soit rien
- **Gestion des Erreurs** : En cas d'échec, aucune donnée partielle n'est sauvegardée

## 🎨 Comportements UI

### Notifications
- **Ajout Examen** : "✅ Examen ajouté à la liste"
- **Suppression Examen** : "🗑️ Examen supprimé avec succès"
- **Sauvegarde Finale** : "✅ Ordre créé avec X examen(s)"

### Compteur Dynamique
- **Mise à Jour Auto** : Le compteur "📊 X examen(s)" s'actualise à chaque ajout/suppression
- **Recherche Intelligente** : Le système retrouve le Span dans le DOM pour le mettre à jour

## 🔍 Gestion des Cas Limites

### 1. **Ordre Non Créé**
- L'utilisateur doit d'abord saisir patient et médecin
- Un ordre temporaire est créé pour permettre l'ajout d'examens

### 2. **Modification d'Ordre**
- Les examens existants sont chargés dans la liste temporaire
- Les modifications ne sont sauvegardées qu'à la validation finale

### 3. **Gestion des Procédures**
- Les procédures sont créées à partir du catalogue
- Sauvegardées uniquement lors de la sauvegarde finale
- Évitent la duplication en cas d'annulation

## 🚀 Améliorations Futures Possibles

1. **Validation en Temps Réel** : Vérifier la cohérence des examens avant sauvegarde
2. **Preview de Sauvegarde** : Montrer un résumé avant la validation finale
3. **Auto-Sauvegarde Brouillon** : Sauvegarder automatiquement les brouillons périodiquement
4. **Gestion des Conflits** : Détecter les doublons ou incohérences

---

**Version** : 2.0  
**Date** : 14/02/2026  
**Auteur** : Assistant IA Cascade  
**Statut** : ✅ Implémenté et testé
