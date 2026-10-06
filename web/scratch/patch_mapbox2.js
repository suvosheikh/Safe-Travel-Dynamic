const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

// Find the useEffect that calls interpolatePoints
const matchRegex = /useEffect\(\(\) => \{\s*\/\*\s*eslint-disable react-hooks\/set-state-in-effect\s*\*\/\s*if \(\!mapRef\.current\)\s*\{\s*setInterpolatedPath\(\[\]\);\s*setSimIndex\(0\);\s*setIsSimulating\(false\);\s*return;\s*\}([\s\S]*?)const path = getSelectedTripPath\(\);/s;

code = code.replace(/useEffect\(\(\) => \{\s*\/\*\s*eslint-disable react-hooks\/set-state-in-effect\s*\*\/[\s\S]*?const path = getSelectedTripPath\(\);/s, `useEffect(() => {
      /* eslint-disable react-hooks/set-state-in-effect */
      const path = getSelectedTripPath();`);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
