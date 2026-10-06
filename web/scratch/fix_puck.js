const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

// 1. Remove the easeTo block that forcefully moves the camera
const easeToRegex = /\/\/ Panning Mapbox Camera to track simulated puck movement smoothly[\s\S]*?\} catch \(err\) \{\}\s*\n\s*\}/g;
code = code.replace(easeToRegex, '');

// 2. Add innerHTML to the traveler marker so it's not invisible!
const markerHtmlTarget = `              el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
              if (isSelected) el.classList.add('selected-pulse');
              
              const tooltip = document.createElement('div');`;

const markerHtmlReplacement = `              el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
              if (isSelected) el.classList.add('selected-pulse');
              
              const puckInner = document.createElement('div');
              puckInner.className = 'gps-navigation-puck mini';
              puckInner.innerHTML = \`
                <div class="puck-pulse-ring \${isSelected ? 'puck-selected' : ''}"></div>
                <div class="puck-core \${isSelected ? 'selected' : ''}">
                   <div class="puck-rotation">
                     <div class="puck-arrow-delta"></div>
                   </div>
                </div>
              \`;
              el.appendChild(puckInner);
              
              const tooltip = document.createElement('div');`;

code = code.replace(markerHtmlTarget, markerHtmlReplacement);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
