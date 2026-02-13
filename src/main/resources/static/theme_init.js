(function() {
    function getTheme() {
        const savedTheme = localStorage.getItem('theme');
        if (savedTheme) {
            return savedTheme;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function setTheme(theme) {
        if (theme === 'dark') {
            document.documentElement.classList.add('theme-dark');
        } else {
            document.documentElement.classList.remove('theme-dark');
        }
        localStorage.setItem('theme', theme);
        const event = new CustomEvent('theme-change', { detail: { theme: theme } });
        document.dispatchEvent(event);
        updateIcon(theme);
    }

    function updateIcon(theme) {
        const btn = document.getElementById('themeToggle');
        if (btn) {
            const icon = btn.querySelector('ion-icon');
            if (icon) {
                icon.setAttribute('name', theme === 'dark' ? 'sunny-outline' : 'moon-outline');
            }
        }
    }

    // Apply immediately
    const initialTheme = getTheme();
    if (initialTheme === 'dark') {
        document.documentElement.classList.add('theme-dark');
    }

    // Expose toggle function
    window.toggleTheme = function() {
        const isDark = document.documentElement.classList.contains('theme-dark');
        setTheme(isDark ? 'light' : 'dark');
    };

    // Setup listener when DOM is ready
    document.addEventListener('DOMContentLoaded', () => {
        const btn = document.getElementById('themeToggle');
        if (btn) {
            updateIcon(document.documentElement.classList.contains('theme-dark') ? 'dark' : 'light');
            btn.addEventListener('click', window.toggleTheme);
        }
    });
})();
