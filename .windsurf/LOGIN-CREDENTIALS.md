# 🔐 Identifiants de Connexion - SAHTY Radiology

## 👤 **Utilisateurs prédéfinis**

### 🏥 **Personnel médical**:

| Rôle | Username | Mot de passe | Nom complet | Email |
|------|----------|--------------|-------------|-------|
| **Administrateur** | `admin` | `admin123` | Admin User | admin@hospital.com |
| **Médecin** | `medecin` | `medecin123` | Jean Dupont | j.dupont@hospital.com |
| **Radiologue** | `radiologue` | `radio123` | Pierre Durand | p.durand@hospital.com |
| **Technicien** | `technicien` | `tech123` | Marie Martin | m.martin@hospital.com |
| **Secrétaire** | `secretaire` | `secret123` | Sophie Lefebvre | s.lefebvre@hospital.com |

## 🚀 **Accès à l'application**

### **URL**: http://localhost:8080

### **Étapes de connexion**:
1. Accédez à http://localhost:8080
2. Si vous n'êtes pas connecté, vous serez redirigé vers la page de login
3. Entrez l'un des identifiants ci-dessus
4. Cliquez sur "Se connecter"

### **Accès direct au login**:
- **URL**: http://localhost:8080/login

## 🔧 **Dépannage**

### **Problème: "Identifiants incorrects"**
✅ **Solution**: Utilisez les identifiants exacts ci-dessus (respectez la casse)

### **Problème: Page blanche**
✅ **Solution**: Attendez le chargement complet de l'application

### **Problème: Boucle de redirection**
✅ **Solution**: Videz le cache du navigateur et réessayez

## 🎯 **Tests recommandés**

### **1. Test administrateur**:
- Username: `admin`
- Mot de passe: `admin123`
- Accès: Toutes les fonctionnalités

### **2. Test médecin**:
- Username: `medecin`
- Mot de passe: `medecin123`
- Accès: Dossiers patients, examens

### **3. Test radiologue**:
- Username: `radiologue`
- Mot de passe: `radio123`
- Accès: Rapports, images médicales

## 🛡️ **Sécurité**

- ✅ **Mots de passe hashés** avec BCrypt
- ✅ **Session sécurisée** avec Spring Security
- ✅ **CSRF protection** activée
- ✅ **WebSocket sécurisé** pour notifications

## 📱 **Après la connexion**

Une fois connecté, vous aurez accès à:
- 📊 **Tableau de bord** principal
- 🏥 **Gestion des patients**
- 📋 **Planning des examens**
- 📈 **Rapports médicaux**
- ⚙️ **Paramètres système**

**L'application est prête pour utilisation!** 🎉
