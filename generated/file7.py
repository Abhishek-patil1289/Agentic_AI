# File: templates/dashboard.html
{% extends "layout.html" %}
{% block content %}
<div class="container">
    <h2>Dashboard</h2>

    <div id="kpi-cards" class="row g-3 mb-4">
        <div class="col-md-3">
            <div class="card text-white bg-primary h-100">
                <div class="card-body">
                    <h5 class="card-title">Total Baseline Hours</h5>
                    <p class="card-text fs-4" id="totalBaselineHours">Loading...</p>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-success h-100">
                <div class="card-body">
                    <h5 class="card-title">Total AI-Assisted Hours</h5>
                    <p class="card-text fs-4" id="totalAiAssistedHours">Loading...</p>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-info h-100">
                <div class="card-body">
                    <h5 class="card-title">Total Hours Saved</h5>
                    <p class="card-text fs-4" id="totalHoursSaved">Loading...</p>
                </div>
            </div>
        </div>
        <div class="col-md-3">
            <div class="card text-white bg-warning h-100">
                <div class="card-body">
                    <h5 class="card-title">Average Savings %</h5>
                    <p class="card-text fs-4" id="averageSavingsPercent">Loading...</p>
                </div>
            </div>
        </div>
    </div>

    <div class="row gy-4">
        <div class="col-lg-6">
            <h5>Effort by SDLC Phase</h5>
            <canvas id="barChart"></canvas>
        </div>

        <div class="col-lg-3">
            <h5>Baseline Effort by Technology</h5>
            <canvas id="pieChart"></canvas>
        </div>

        <div class="col-lg-3">
            <h5>Hours Saved by Business Unit</h5>
            <canvas id="doughnutChart"></canvas>
        </div>
    </div>
</div>

<script>
document.addEventListener('DOMContentLoaded', async () => {
    // Load KPIs
    const kpiResponse = await fetch('/api/projects/analytics/kpis');
    const kpis = await kpiResponse.json();
    document.getElementById('totalBaselineHours').textContent = kpis.totalBaselineEffortHours.toFixed(2);
    document.getElementById('totalAiAssistedHours').textContent = kpis.totalAiAssistedEffortHours.toFixed(2);
    document.getElementById('totalHoursSaved').textContent = kpis.totalHoursSaved.toFixed(2);
    document.getElementById('averageSavingsPercent').textContent = kpis.averageSavingsPercent.toFixed(2) + '%';

    // Bar chart: effort by SDLC phase
    const barCtx = document.getElementById('barChart').getContext('2d');
    const barDataResp = await fetch('/api/projects/analytics/effort-by-sdlc-phase');
    const barDataJson = await barDataResp.json();

    const barLabels = Object.keys(barDataJson);
    const baselineData = barLabels.map(l => barDataJson[l].baselineEffortHours);
    const aiAssistedData = barLabels.map(l => barDataJson[l].aiAssistedEffortHours);
    const hoursSavedData = barLabels.map(l => barDataJson[l].hoursSaved);

    const barChart = new Chart(barCtx, {
        type: 'bar',
        data: {
            labels: barLabels,
            datasets: [
                { label: 'Baseline Effort', data: baselineData, backgroundColor: 'rgba(54, 162, 235, 0.7)' },
                { label: 'AI-Assisted Effort', data: aiAssistedData, backgroundColor: 'rgba(75, 192, 192, 0.7)' },
                { label: 'Hours Saved', data: hoursSavedData, backgroundColor: 'rgba(255, 206, 86, 0.7)' }
            ]
        },
        options: {
            responsive: true,
            scales: {
                y: { beginAtZero: true }
            }
        }
    });

    // Pie chart: baseline effort by technology
    const pieCtx = document.getElementById('pieChart').getContext('2d');
    const pieDataResp = await fetch('/api/projects/analytics/effort-by-technology');
    const pieDataJson = await pieDataResp.json();

    const pieLabels = Object.keys(pieDataJson);
    const pieDataValues = pieLabels.map(l => pieDataJson[l].baselineEffortHours);

    const pieChart = new Chart(pieCtx, {
        type: 'pie',
        data: {
            labels: pieLabels,
            datasets: [{
                data: pieDataValues,
                backgroundColor: pieLabels.map(() => `hsl(${Math.random() * 360}, 70%, 60%)`),
                borderWidth: 1
            }]
        },
        options: { responsive: true }
    });

    // Doughnut chart: hours saved by business unit
    const doughnutCtx = document.getElementById('doughnutChart').getContext('2d');
    const doughnutDataResp = await fetch('/api/projects/analytics/effort-by-business-unit');
    const doughnutDataJson = await doughnutDataResp.json();

    const doughnutLabels = Object.keys(doughnutDataJson);
    const doughnutDataValues = doughnutLabels.map(l => doughnutDataJson[l].hoursSaved);

    const doughnutChart = new Chart(doughnutCtx, {
        type: 'doughnut',
        data: {
            labels: doughnutLabels,
            datasets: [{
                data: doughnutDataValues,
                backgroundColor: doughnutLabels.map(() => `hsl(${Math.random() * 360}, 70%, 60%)`),
                borderWidth: 1
            }]
        },
        options: { responsive: true }
    });
});
</script>
{% endblock %}