let clickChart, deviceChart, countryChart;

document.addEventListener('DOMContentLoaded', () => {
    fetchProfile();
    fetchLinks();
    setupTabs();
    initCharts();
});

async function authenticatedFetch(url, options = {}) {
    const apiKey = localStorage.getItem('apiKey');
    const apiSecret = localStorage.getItem('apiSecret');
    
    // Support Session-based auth (Cookies) if no API key
    // We do not redirect here anymore if keys are missing.
    
    const headers = { ...options.headers };
    
    if (apiKey && apiSecret) {
        headers['X-API-KEY'] = apiKey;
        headers['X-API-SECRET'] = apiSecret;
    }
    
    // Ensure Content-Type is set for POST/PUT if body exists
    if (!headers['Content-Type'] && options.body) {
        headers['Content-Type'] = 'application/json';
    }

    const newOptions = { ...options, headers };
    
    const response = await fetch(url, newOptions);
    
    if (response.status === 401 || response.status === 403) {
        // Clear keys if they existed, as they might be invalid
        localStorage.removeItem('apiKey');
        localStorage.removeItem('apiSecret');
        
        // Redirect to login only if we were trying to access a protected resource
        // AND we are not already on the login page (which we aren't, this is dashboard.js)
        window.location.href = '/';
        throw new Error('Session expired or not authenticated');
    }
    
    return response;
}

// Chart Defaults
const chartColors = {
    light: {
        text: '#64748b',
        grid: '#e2e8f0',
        primary: '#4f46e5',
        bg: 'rgba(79, 70, 229, 0.1)',
        success: '#10b981'
    },
    dark: {
        text: '#94a3b8',
        grid: '#334155',
        primary: '#6366f1',
        bg: 'rgba(99, 102, 241, 0.2)',
        success: '#34d399'
    }
};

function getCurrentTheme() {
    return document.documentElement.classList.contains('theme-dark') ? 'dark' : 'light';
}

function initCharts() {
    const historyCtx = document.getElementById('clickHistoryChart');
    if (!historyCtx) return;

    if (clickChart) clickChart.destroy();
    if (deviceChart) deviceChart.destroy();
    if (countryChart) countryChart.destroy();

    const theme = getCurrentTheme();
    const colors = chartColors[theme];

    // Common Chart Options
    const commonOptions = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: { labels: { color: colors.text } },
            title: { color: colors.text }
        },
        scales: {
            x: {
                grid: { color: colors.grid },
                ticks: { color: colors.text }
            },
            y: {
                grid: { color: colors.grid },
                ticks: { color: colors.text }
            }
        }
    };

    // Click History Chart
    clickChart = new Chart(historyCtx.getContext('2d'), {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'Clicks',
                data: [],
                borderColor: colors.primary,
                backgroundColor: colors.bg,
                fill: true,
                tension: 0.4,
                pointBackgroundColor: colors.primary
            }]
        },
        options: {
            ...commonOptions,
            plugins: { legend: { display: false } }
        }
    });

    // Device Chart (Doughnut - no axes)
    deviceChart = new Chart(document.getElementById('deviceChart').getContext('2d'), {
        type: 'doughnut',
        data: {
            labels: ['Desktop', 'Mobile'],
            datasets: [{
                data: [0, 0],
                backgroundColor: [colors.primary, colors.success],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { color: colors.text } }
            }
        }
    });

    // Country Chart
    countryChart = new Chart(document.getElementById('countryChart').getContext('2d'), {
        type: 'bar',
        data: {
            labels: [],
            datasets: [{
                label: 'Clicks',
                data: [],
                backgroundColor: colors.primary,
                borderRadius: 4
            }]
        },
        options: {
            ...commonOptions,
            plugins: { legend: { display: false } },
            scales: {
                x: { ...commonOptions.scales.x, grid: { display: false } }, // Cleaner look
                y: commonOptions.scales.y
            }
        }
    });
}

// Listen for theme changes (dispatched by theme_init.js)
document.addEventListener('theme-change', () => {
    // Re-init charts with new colors, preserving data if available
    // For simplicity, we just re-fetch or re-render if data is stored globally.
    // Ideally, update chart options directly, but re-init ensures all defaults are applied.
    // If data is in variables, we can just updateOptions.
    
    // For now, let's just update the options/colors of existing charts without destroying if possible,
    // or destroy and rebuild with current data.
    if (clickChart && deviceChart && countryChart) {
        initCharts();
        // We need to re-populate data...
        // Let's just refetch currently active view or rely on state.
        // A simple way is to re-fetch global or specific stats.
        // Or store last data.
        
        // Let's trigger a refresh based on active tab
        const activeTab = document.querySelector('.menu-link.active').dataset.tab;
        if (activeTab === 'dashboard') {
             fetchGlobalAnalytics(); 
        } else if (activeTab === 'analytics') {
            // We don't know which shortCode was last viewed easily unless we stored it.
            // For now, dashboard stats are safe to reload.
        }
    }
});

function updateCharts(analytics) {
    if (!clickChart) return;

    // Timeline
    const dates = Object.keys(analytics.clicksByDate).sort();
    clickChart.data.labels = dates;
    clickChart.data.datasets[0].data = dates.map(d => analytics.clicksByDate[d]);
    clickChart.update();

    // Devices
    const devices = analytics.clicksByDevice;
    deviceChart.data.datasets[0].data = [devices.desktop || 0, devices.mobile || 0];
    deviceChart.update();

    // Countries
    const countries = Object.keys(analytics.clicksByCountry).sort((a,b) => analytics.clicksByCountry[b] - analytics.clicksByCountry[a]).slice(0, 5);
    countryChart.data.labels = countries;
    countryChart.data.datasets[0].data = countries.map(c => analytics.clicksByCountry[c]);
    countryChart.update();
}

async function fetchAnalytics(shortCode) {
    try {
        const fullShortCode = shortCode.split('/').pop();
        const res = await authenticatedFetch(`/api/stats/${fullShortCode}`);
        if (!res.ok) throw new Error('Failed to load analytics');
        const data = await res.json();
        updateCharts(data.analytics);
    } catch (e) {
        console.error(e);
    }
}

// Global Aggregation (Optimized)
async function fetchGlobalAnalytics() {
    try {
        const res = await authenticatedFetch('/api/dashboard/stats');
        if (!res.ok) throw new Error('Failed to load global analytics');
        const data = await res.json();
        updateCharts(data);
    } catch (e) {
        console.error('Global analytics error:', e);
    }
}

function setupTabs() {
    const menuLinks = document.querySelectorAll('.menu-link[data-tab]');
    const contents = document.querySelectorAll('.tab-content');

    menuLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = link.getAttribute('data-tab');

            menuLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            contents.forEach(c => {
                if (c.id === tabId) {
                    c.classList.remove('hidden');
                } else {
                    c.classList.add('hidden');
                }
            });
        });
    });
}

async function fetchProfile() {
    try {
        // OAuth2 Bootstrap: Check if we need to fetch API keys from session
        if (!localStorage.getItem('apiKey')) {
            const keyRes = await fetch('/api/auth/keys');
            if (keyRes.ok) {
                const keyData = await keyRes.json();
                localStorage.setItem('apiKey', keyData.apiKey);
                localStorage.setItem('apiSecret', keyData.apiSecret);
            }
        }
    
        const res = await authenticatedFetch('/api/dashboard/user');
        if (!res.ok) throw new Error('Failed to load profile');
        const user = await res.json();
        
        document.getElementById('profName').value = user.fullName;
        document.getElementById('profEmail').value = user.email;
    } catch (e) {
        console.error(e);
        // Do not redirect here; authenticatedFetch handles auth errors. 
        // Other errors should just be logged/shown to user.
    }
}

async function fetchLinks() {
    try {
        const res = await authenticatedFetch('/api/dashboard/links');
        if (!res.ok) throw new Error('Failed to load links');
        const links = await res.json();
        
        const tbody = document.getElementById('linksTableBody');
        tbody.innerHTML = '';
        
        let totalClicks = 0;
        
        links.forEach(link => {
            totalClicks += link.clickCount;
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="col-original" title="${link.originalUrl}">${link.originalUrl}</td>
                <td><a href="${link.shortUrl}" target="_blank" style="color:var(--accent-primary); text-decoration:none; font-weight:600;">${link.shortUrl}</a></td>
                <td>${link.clickCount}</td>
                <td>${new Date(link.createdAt).toLocaleDateString()}</td>
                <td>
                    <button class="action-btn" onclick="copyLink('${link.shortUrl}')" title="Copy"><ion-icon name="copy-outline"></ion-icon></button>
                    <button class="action-btn ${link.active ? '' : 'disabled-state'}" onclick="toggleLink(${link.id})" title="${link.active ? 'Disable' : 'Enable'}">
                        <ion-icon name="${link.active ? 'eye-outline' : 'eye-off-outline'}"></ion-icon>
                    </button>
                    <button class="action-btn" onclick="editLink(${JSON.stringify(link).replace(/"/g, '&quot;')})" title="Edit"><ion-icon name="create-outline"></ion-icon></button>
                    <button class="action-btn" onclick="showLinkStats('${link.shortCode}')" title="Stats"><ion-icon name="stats-chart-outline"></ion-icon></button>
                    <button class="action-btn" style="color:#ef4444;" onclick="deleteLink(${link.id})" title="Delete"><ion-icon name="trash-outline"></ion-icon></button>
                </td>
            `;
            tbody.appendChild(tr);
        });
        
        // Update Analytics Grid
        document.getElementById('totalLinks').textContent = links.length;
        document.getElementById('totalClicks').textContent = totalClicks;

        // Load Global Analytics (Optimized)
        fetchGlobalAnalytics();
        
    } catch (e) {
        console.error(e);
    }
}

function showLinkStats(shortCode) {
    // Switch to analytics tab
    const analyticsTabBtn = document.querySelector('.menu-link[data-tab="analytics"]');
    if (analyticsTabBtn) analyticsTabBtn.click();
    
    // Fetch for this link
    fetchAnalytics(shortCode);
}

let currentEditingLinkId = null;

function editLink(link) {
    currentEditingLinkId = link.id;
    document.getElementById('smartLongUrl').value = link.originalUrl;
    document.getElementById('smartAlias').value = link.shortCode;
    document.getElementById('smartPassword').value = ""; // Don't show hashed password
    document.getElementById('smartMaxClicks').value = link.maxClicks || "";
    document.getElementById('smartGeoRules').value = link.geoRules || "";
    document.getElementById('smartDeviceRules').value = link.deviceRules || "";
    
    // Update Modal UI
    document.querySelector('#createLinkModal h2').textContent = "Update Smart Link";
    document.getElementById('submitSmartLink').textContent = "Update Link";
    document.getElementById('smartAlias').disabled = true; // Cannot change alias after creation
    
    createLinkModal.classList.remove('hidden');
}

async function toggleLink(id) {
    try {
        const res = await authenticatedFetch(`/api/links/${id}/toggle`, { method: 'POST' });
        if (res.ok) fetchLinks();
    } catch (e) { console.error(e); }
}

async function deleteLink(id) {
    if (!confirm('Are you sure you want to delete this link? This action cannot be undone.')) return;
    try {
        const res = await authenticatedFetch(`/api/links/${id}`, { method: 'DELETE' });
        if (res.ok) fetchLinks();
    } catch (e) { console.error(e); }
}

function copyLink(url) {
    navigator.clipboard.writeText(url);
    alert('Copied to clipboard!');
}

// --- Create Smart Link Modal Logic ---
const createLinkBtn = document.getElementById('createLinkBtn');
const createLinkModal = document.getElementById('createLinkModal');
const closeCreateModal = document.getElementById('closeCreateModal');
const submitSmartLink = document.getElementById('submitSmartLink');
const tabBtns = document.querySelectorAll('.tab-btn');
const tabContents = document.querySelectorAll('.modal-tab-content');

if (createLinkBtn) {
    createLinkBtn.onclick = () => {
        currentEditingLinkId = null;
        document.querySelector('#createLinkModal h2').textContent = "Create Smart Link";
        document.getElementById('submitSmartLink').textContent = "Create Link";
        document.getElementById('smartAlias').disabled = false;
        
        // Reset fields
        document.getElementById('smartLongUrl').value = "";
        document.getElementById('smartAlias').value = "";
        document.getElementById('smartPassword').value = "";
        document.getElementById('smartMaxClicks').value = "";
        document.getElementById('smartGeoRules').value = "";
        document.getElementById('smartDeviceRules').value = "";

        createLinkModal.classList.remove('hidden');
    };
}

if (closeCreateModal) {
    closeCreateModal.onclick = () => {
        createLinkModal.classList.add('hidden');
    };
}

// Tab Switching
tabBtns.forEach(btn => {
    btn.addEventListener('click', () => {
        // Remove active class from all
        tabBtns.forEach(b => b.classList.remove('active'));
        tabContents.forEach(c => c.classList.add('hidden'));

        // Add active to current
        btn.classList.add('active');
        const targetId = btn.getAttribute('data-target');
        document.getElementById(targetId).classList.remove('hidden');
    });
});

// Submit Logic
if (submitSmartLink) {
    submitSmartLink.onclick = async () => {
        const longUrl = document.getElementById('smartLongUrl').value;
        const password = document.getElementById('smartPassword').value;
        const expiryDate = document.getElementById('smartExpiry').value;
        const maxClicks = document.getElementById('smartMaxClicks').value;
        const geoRules = document.getElementById('smartGeoRules').value;
        const deviceRules = document.getElementById('smartDeviceRules').value;

        if (!longUrl) {
            alert('Please enter a Destination URL');
            return;
        }

        let expiresInDays = 30;
        if (expiryDate) {
            const end = new Date(expiryDate);
            const now = new Date();
            const diffTime = Math.abs(end - now);
            expiresInDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)); 
        }

        const customAlias = document.getElementById('smartAlias').value;

        const payload = {
            longUrl: longUrl,
            expiresInDays: expiresInDays,
            maxClicks: maxClicks ? parseInt(maxClicks) : null,
            password: password || null,
            geoRules: geoRules || null,
            deviceRules: deviceRules || null,
            customAlias: customAlias || null
        };

        const method = currentEditingLinkId ? 'PUT' : 'POST';
        const url = currentEditingLinkId ? `/api/links/${currentEditingLinkId}` : '/api/shorten';

        try {
            const res = await authenticatedFetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.message || 'Failed to process link');
            }

            const data = await res.json();
            alert(currentEditingLinkId ? 'Link updated successfully!' : 'Link created successfully!');
            createLinkModal.classList.add('hidden');
            fetchLinks(); // Refresh list
        } catch (e) {
            alert('Error: ' + e.message);
        }
    };
}
