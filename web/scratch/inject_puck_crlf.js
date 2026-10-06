const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

code = code.replace(/\r\n/g, '\n');

const target = `          } else {
            try {
              const el = document.createElement('div');
              el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
              if (isSelected) el.classList.add('selected-pulse');
              
              const tooltip = document.createElement('div');`;

const replacement = `          } else {
            try {
              const el = document.createElement('div');
              el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
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

code = code.replace(target, replacement);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
