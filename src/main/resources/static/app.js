const shortenBtn = document.getElementById('shortenBtn');
const longUrlInput = document.getElementById('longUrl');
const resultArea = document.getElementById('resultArea');
const errorArea = document.getElementById('errorArea');
const shortLink = document.getElementById('shortLink');
const statsLink = document.getElementById('statsLink');
const copyBtn = document.getElementById('copyBtn');
const errorMessage = document.getElementById('errorMessage');


// Auth Elements
const authModal = document.getElementById('authModal');
const loginForm = document.getElementById('loginForm');
const signupForm = document.getElementById('signupForm');
const showSignupBtn = document.getElementById('showSignup');
const showLoginBtn = document.getElementById('showLogin');
const closeModal = document.querySelector('.close-modal');
const navLogin = document.getElementById('navLoginBtn');
const navGuest = document.getElementById('navGuestBtn');
const navCta = document.querySelector('.btn-pill'); // Get Started

// Guest Mode Buttons in Modal
const guestLoginBtn = document.getElementById('guestLoginBtn');
const guestSignupBtn = document.getElementById('guestSignupBtn');

// --- Guest Mode Logic ---
const handleGuestMode = (e) => {
    if (e) e.preventDefault();
    authModal.classList.add('hidden');
    sessionStorage.setItem('guestMode', 'true');
    updateGuestUI();
    // Scroll to shortening input
    document.querySelector('.command-center-pro').scrollIntoView({ behavior: 'smooth' });
    longUrlInput.focus();
};

function updateGuestUI() {
    const isGuest = sessionStorage.getItem('guestMode') === 'true';
    const guestBadge = document.getElementById('guestBadge');
    const customAliasField = document.getElementById('customAlias');
    const customAliasContainer = customAliasField.closest('.setting-group');

    if (isGuest) {
        if (navGuest) navGuest.textContent = "Guest Mode Active";
        if (guestBadge) guestBadge.classList.remove('hidden');
        if (customAliasContainer) {
            customAliasContainer.classList.add('guest-restricted');
            customAliasField.disabled = true;
            customAliasField.placeholder = "Login to use custom aliases";
        }
    } else {
        if (navGuest) navGuest.textContent = "Guest Mode";
        if (guestBadge) guestBadge.classList.add('hidden');
        if (customAliasContainer) {
            customAliasContainer.classList.remove('guest-restricted');
            customAliasField.disabled = false;
            customAliasField.placeholder = "custom-name";
        }
    }
}

if (navGuest) navGuest.addEventListener('click', handleGuestMode);
if (guestLoginBtn) guestLoginBtn.addEventListener('click', handleGuestMode);
if (guestSignupBtn) guestSignupBtn.addEventListener('click', handleGuestMode);



// Initial Load

// Check Session
if (localStorage.getItem('apiKey')) {
    window.location.href = '/dashboard.html';
}

updateGuestUI();

const pasteBtn = document.getElementById('pasteBtn');
const toggleDrawerBtn = document.getElementById('toggleDrawerBtn');
const commandDrawer = document.getElementById('commandDrawer');

// Toggle Drawer
if(toggleDrawerBtn) {
    toggleDrawerBtn.addEventListener('click', () => {
        commandDrawer.classList.toggle('hidden');
    });
}

// Paste Logic
if(pasteBtn) {
    pasteBtn.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            longUrlInput.value = text;
        } catch (err) {
            alert('Failed to read clipboard permissions');
        }
    });
}

shortenBtn.addEventListener('click', async () => {
    let originalUrl = longUrlInput.value.trim();
    if (!originalUrl) return;

    // UTM Builder Logic
    const utmSource = document.getElementById('utmSource').value.trim();
    const utmMedium = document.getElementById('utmMedium').value.trim();
    const utmCampaign = document.getElementById('utmCampaign').value.trim();
    
    if (utmSource || utmMedium || utmCampaign) {
        const urlObj = new URL(originalUrl); // Validates URL too
        if (utmSource) urlObj.searchParams.set('utm_source', utmSource);
        if (utmMedium) urlObj.searchParams.set('utm_medium', utmMedium);
        if (utmCampaign) urlObj.searchParams.set('utm_campaign', utmCampaign);
        originalUrl = urlObj.toString();
    }

    const customAlias = document.getElementById('customAlias').value.trim();

    // Reset UI
    resultArea.classList.add('hidden');
    errorArea.classList.add('hidden');
    shortenBtn.disabled = true;
    shortenBtn.textContent = 'Shortening...';

    try {
        const payload = { 
            longUrl: originalUrl,
            customAlias: customAlias || null
        };

        const response = await fetch('/api/shorten', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const errData = await response.json();
            throw new Error(errData.message || 'Something went wrong');
        }

        const data = await response.json();
        const shortUrl = window.location.origin + '/' + data.shortCode;

        shortLink.href = shortUrl;
        shortLink.textContent = shortUrl;
        


        // If it was a guest shorten, show a reminder about 7-day expiry
        if (sessionStorage.getItem('guestMode') === 'true') {
            const expiryNotice = document.createElement('p');
            expiryNotice.className = 'expiry-notice';
            expiryNotice.innerHTML = 'Guest link: Expires in 7 days. <a href="#" onclick="openModal(); return false;">Login to save</a>';
            resultArea.appendChild(expiryNotice);
        }

        statsLink.onclick = (e) => {
            e.preventDefault();
            fetchStats(data.shortCode);
        };

        resultArea.classList.remove('hidden');
    } catch (error) {
        errorMessage.textContent = error.message;
        errorArea.classList.remove('hidden');
    } finally {
        shortenBtn.disabled = false;
        shortenBtn.textContent = 'Shorten';
    }
});

copyBtn.addEventListener('click', () => {
    navigator.clipboard.writeText(shortLink.textContent)
        .then(() => {
            const btnHtml = copyBtn.innerHTML;
            copyBtn.innerHTML = '<ion-icon name="checkmark-outline"></ion-icon>';
            setTimeout(() => {
                copyBtn.innerHTML = '<ion-icon name="copy-outline"></ion-icon>';
            }, 2000);
        });
});

async function fetchStats(shortCode) {
    try {
        const response = await fetch(`/api/stats/${shortCode}`);
        const data = await response.json();
        alert(`Original URL: ${data.originalUrl}\nClicks: ${data.clickCount}`);
    } catch (e) {
        alert('Failed to fetch stats');
    }
}

// --- Auth UI Logic ---
function openModal() {
    authModal.classList.remove('hidden');
    loginForm.classList.remove('hidden');
    signupForm.classList.add('hidden');
}

if(navLogin) {
    navLogin.onclick = (e) => {
        e.preventDefault();
        openModal();
    };
}
if(navCta) {
    navCta.onclick = (e) => {
        e.preventDefault();
        openModal();
    };
}

closeModal.onclick = () => {
    authModal.classList.add('hidden');
};

showSignupBtn.onclick = () => {
    loginForm.classList.add('hidden');
    signupForm.classList.remove('hidden');
};

showLoginBtn.onclick = () => {
    signupForm.classList.add('hidden');
    loginForm.classList.remove('hidden');
};

// Close modal on outside click
window.onclick = (event) => {
    if (event.target == authModal) {
        authModal.classList.add('hidden');
    }
};

// --- Auth API Integration ---

// Signup
// Signup
signupForm.querySelector('.btn-primary').onclick = async (e) => {
    e.preventDefault(); // Stop form submission
    console.log('[Auth] Signup triggered');
    const inputs = signupForm.querySelectorAll('input');
    const fullName = inputs[0].value;
    const email = inputs[1].value;
    const password = inputs[2].value;

    try {
        const response = await fetch('/api/auth/signup', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, email, password })
        });
        
        const data = await response.json();
        console.log('[Auth] Signup response:', data);
        
        if (response.ok) {
            alert(data.message);
            // Switch to login
            signupForm.classList.add('hidden');
            loginForm.classList.remove('hidden');
        } else {
            showAuthError(signupForm, data.message || 'Signup failed');
        }
    } catch (e) {
        console.error('[Auth] Signup error:', e);
        showAuthError(signupForm, e.message);
    }
};

function showAuthError(container, message) {
    let errDiv = container.querySelector('.auth-error');
    if (!errDiv) {
        errDiv = document.createElement('div');
        errDiv.className = 'auth-error';
        errDiv.style.color = '#ef4444';
        errDiv.style.fontSize = '0.85rem';
        errDiv.style.marginTop = '0.5rem';
        errDiv.style.textAlign = 'center';
        container.insertBefore(errDiv, container.querySelector('p.switch-auth'));
    }
    errDiv.textContent = message;
}

// Login
// Login
loginForm.querySelector('.btn-primary').onclick = async (e) => {
    e.preventDefault(); // Stop form submission
    console.log('[Auth] Login triggered');
    const inputs = loginForm.querySelectorAll('input');
    const email = inputs[0].value;
    const password = inputs[1].value;

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });
        
        const data = await response.json();
        console.log('[Auth] Login response:', data);
        
        if (response.ok) {
            console.log('[Auth] Login successful, redirecting...');
            
            // Store Auth Credentials
            localStorage.setItem('apiKey', data.apiKey);
            localStorage.setItem('apiSecret', data.apiSecret);

            authModal.classList.add('hidden');
            // Update UI to show logged in state if desired (e.g. change "Login" text)
             if(navLogin) {
                  navLogin.textContent = "My Account";
                  navLogin.href = "/dashboard.html";
                  navLogin.onclick = null; // Remove modal trigger
             }
             sessionStorage.removeItem('guestMode');
             
             // Directly redirect without delay
             window.location.replace("/dashboard.html"); 
        } else {
            showAuthError(loginForm, data.message || 'Invalid credentials');
        }
    } catch (e) {
        console.error('[Auth] Login error:', e);
        showAuthError(loginForm, e.message);
    }
};
