
// --- Auth API Integration ---

// Signup
signupForm.querySelector('button').onclick = async () => {
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
        
        if (response.ok) {
            alert(data.message);
            // Switch to login
            signupForm.classList.add('hidden');
            loginForm.classList.remove('hidden');
        } else {
            alert(data.message || 'Signup failed');
        }
    } catch (e) {
        alert('Signup error: ' + e.message);
    }
};

// Login
loginForm.querySelector('button').onclick = async () => {
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
        
        if (response.ok) {
            alert(data.message);
            authModal.classList.add('hidden');
            // Update UI to show logged in state if desired (e.g. change Login button to Avatar)
            // For now just close modal
        } else {
            alert('Login failed: ' + (data.message || 'Invalid credentials'));
        }
    } catch (e) {
        alert('Login error: ' + e.message);
    }
};
