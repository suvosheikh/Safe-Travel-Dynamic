const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

const regex = /(el\.classList\.add\('selected-pulse'\);[\s\S]*?)(const tooltip = document\.createElement\('div'\);)/;

const match = code.match(regex);
if (match && !code.includes('puckInner')) {
  code = code.replace(regex, `$1
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

              $2`);
  fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
  console.log("Applied!");
} else {
  console.log("Not found or already applied.");
}
