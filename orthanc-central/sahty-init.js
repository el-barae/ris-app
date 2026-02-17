// ===================================================================
// SAHTY Viewer - Script Minimal
// Change uniquement le titre et ajoute le header avec logo
// Garde tout le reste du style OHIF par défaut
// ===================================================================

(function() {
    'use strict';

    console.log('🏥 SAHTY Viewer - Mode Minimal');

    // ===================================================================
    // 1. CHARGER LE CSS MINIMAL
    // ===================================================================
    function injectMinimalCSS() {
        if (document.getElementById('sahty-minimal-css')) {
            return;
        }

        const link = document.createElement('link');
        link.id = 'sahty-minimal-css';
        link.rel = 'stylesheet';
        link.href = '/sahty-custom-styles.css';
        link.onload = () => console.log('✅ CSS minimal chargé');

        document.head.appendChild(link);
    }

    // ===================================================================
    // 2. CHANGER LE TITRE DE LA PAGE ET LE FAVICON
    // ===================================================================
    function updatePageTitle() {
        document.title = '🏥 SAHTY Viewer';
        console.log('✅ Titre mis à jour');
    }

    function updateFavicon() {
        // Supprimer l'ancien favicon
        const oldFavicon = document.querySelector("link[rel*='icon']");
        if (oldFavicon) {
            oldFavicon.remove();
        }

        // Ajouter le nouveau favicon SAHTY
        const favicon = document.createElement('link');
        favicon.rel = 'icon';
        favicon.type = 'image/jpeg';
        favicon.href = '/sahty.jpeg';
        document.head.appendChild(favicon);

        console.log('✅ Favicon SAHTY appliqué');
    }

    // ===================================================================
    // 3. AJOUTER LE HEADER SAHTY
    // ===================================================================
    function injectSahtyHeader() {
        // Vérifier si déjà présent
        if (document.querySelector('.sahty-custom-header')) {
            return;
        }

        const header = document.createElement('div');
        header.className = 'sahty-custom-header';

        header.innerHTML = `
      <div class="sahty-logo">
        <img src="/sahty.jpeg" 
             alt="SAHTY Logo"
             onerror="this.style.display='none';">
        <div>
          <h1>SAHTY Viewer</h1>
          <p class="sahty-subtitle">Imagerie Médicale</p>
        </div>
      </div>
      <div style="color: #2ecc71; font-weight: 600; font-size: 0.85rem;">
        ● En ligne
      </div>
    `;

        // Insérer en haut de la page
        if (document.body.firstChild) {
            document.body.insertBefore(header, document.body.firstChild);
        } else {
            document.body.appendChild(header);
        }

        console.log('✅ Header SAHTY ajouté');
    }

    // ===================================================================
    // 4. CHANGER LE TEXTE DE LA NOTIFICATION
    // ===================================================================
    function updateNotificationText() {
        // Chercher la notification
        const checkNotification = setInterval(() => {
            const alert = document.querySelector('[role="alert"]');
            if (alert) {
                // Trouver le texte principal
                const mainText = alert.querySelector('p');
                if (mainText && mainText.textContent.includes('OHIF')) {
                    mainText.textContent = 'SAHTY Viewer - Système d\'imagerie médicale professionnel';
                }

                // Trouver le lien
                const link = alert.querySelector('a');
                if (link && link.textContent.includes('OHIF')) {
                    link.textContent = 'En savoir plus sur SAHTY Viewer';
                    // Optionnel: changer le lien
                    // link.href = 'https://votre-site.com';
                }

                clearInterval(checkNotification);
                console.log('✅ Notification mise à jour');
            }
        }, 500);

        // Arrêter après 10 secondes si pas trouvé
        setTimeout(() => clearInterval(checkNotification), 10000);
    }

    // ===================================================================
    // 5. CACHER L'ANCIEN HEADER OHIF
    // ===================================================================
    function hideOHIFHeader() {
        const style = document.createElement('style');
        style.id = 'hide-ohif-header';
        style.textContent = `
      [class*="Header-module"]:not(.sahty-custom-header),
      [class*="header-module"]:not(.sahty-custom-header),
      header[class*="Header"]:not(.sahty-custom-header) {
        display: none !important;
      }
    `;
        document.head.appendChild(style);
        console.log('✅ Header OHIF caché');
    }

    // ===================================================================
    // 6. INITIALISATION
    // ===================================================================
    function init() {
        console.log('🚀 Initialisation SAHTY...');

        // Charger le CSS minimal
        injectMinimalCSS();

        // Changer le titre et favicon
        updatePageTitle();
        updateFavicon();

        // Cacher l'ancien header
        hideOHIFHeader();

        // Ajouter le header SAHTY
        setTimeout(injectSahtyHeader, 500);

        // Changer le texte de la notification
        setTimeout(updateNotificationText, 1000);

        // Vérifier et réinjecter si nécessaire
        setTimeout(injectSahtyHeader, 2000);
        setTimeout(updateNotificationText, 3000);

        console.log('✅ SAHTY Viewer initialisé');
    }

    // ===================================================================
    // 7. LANCEMENT
    // ===================================================================

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.addEventListener('load', () => {
        setTimeout(init, 500);
    });

    console.log('%c🏥 SAHTY Viewer - Mode Minimal', 'background: #2ecc71; color: white; font-size: 14px; padding: 8px 16px; border-radius: 4px;');
    console.log('%cStyle OHIF par défaut conservé', 'color: #95a5a6; font-size: 11px;');

})();