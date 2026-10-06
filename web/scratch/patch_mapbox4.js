const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

const haversineCode = `
  const toRad = (x: number) => (x * Math.PI) / 180;
  const haversine = (coords1: [number, number], coords2: [number, number]) => {
    if (!coords1 || !coords2) return 0;
    const R = 3958.8; // Radius of earth in miles
    const dLat = toRad(coords2[1] - coords1[1]);
    const dLon = toRad(coords2[0] - coords1[0]);
    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(toRad(coords1[1])) * Math.cos(toRad(coords2[1])) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };
  
  const animationRef = useRef<number>(undefined);
`;

code = code.replace(/const animationRef = useRef<number>\(\);/, haversineCode);

// Also I see the error is still Cannot find name 'haversine'. Wait, is there a second `const animationRef = useRef<number>(null);`?
// Let's replace the one that exists.
code = code.replace(/const animationRef = useRef<number>\((null)?\);/, haversineCode);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
