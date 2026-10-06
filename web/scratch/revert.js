const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

// 1. Restore simIndex state
code = code.replace(/const simIndexRef = useRef<number>\(0\);/, 'const [simIndex, setSimIndex] = useState<number>(0);');

// 2. Change interpolatePoints density back to 150
code = code.replace(/interpolatePoints\(path, 1500\)/g, 'interpolatePoints(path, 150)');

// 3. Replace the simIndexRef assignments
code = code.replace(/simIndexRef\.current = 0;/g, 'setSimIndex(0);');

// 4. Replace the rAF loop with the old setInterval loop
const rafRegex = /\/\/ 2\. High-Performance 60 FPS requestAnimationFrame Simulation Loop[\s\S]*?\}, \[isSimulating, interpolatedPath, simSpeed, selectedTripId, activeTripsList\]\);/;

const intervalCode = `// 2. Active timer simulation loop
  useEffect(() => {
    if (!isSimulating || interpolatedPath.length === 0) return;

    const intervalTime = Math.max(80, 425 / simSpeed); // adapt intervals to multiplier
    const timer = setInterval(() => {
      setSimIndex((prev) => {
        if (prev >= interpolatedPath.length - 1) {
          return 0; // Seamless loop to demo forever
        }
        return prev + 1;
      });
    }, intervalTime);

    return () => clearInterval(timer);
  }, [isSimulating, interpolatedPath, simSpeed]);`;

code = code.replace(rafRegex, intervalCode);

// 5. Replace remaining simIndexRef.current with simIndex
code = code.replace(/simIndexRef\.current/g, 'simIndex');

// 6. Restore the mapRef.current.easeTo inside the marker generation block
const targetMarkerBlock = `const routeCoords = extractRouteLog(trip.route_path_log) || roadGeometries[key] || generateSimulatedRoadRoute(coords, endCoords);`;

const newMarkerBlock = `const routeCoords = extractRouteLog(trip.route_path_log) || roadGeometries[key] || generateSimulatedRoadRoute(coords, endCoords);

      // Panning Mapbox Camera to track simulated puck movement smoothly
      if (isSimActive) {
        const current = interpolatedPath[simIndex];
        const next = interpolatedPath[simIndex + 1] || current;
        const heading = (current && next) ? calculateSimpleHeading(current, next) : 0;

        try {
          mapRef.current.easeTo({
            center: coords,
            zoom: 15.2,
            pitch: 52, // Tilt camera for a gorgeous 3D immersive navigation bird's eye view
            bearing: heading,
            duration: Math.max(80, 1100 / simSpeed), // Match duration with simulation stepping dynamically
            easing: (t: number) => t, // Linear pacing for continuous movement without start/stop jerkiness
            essential: true
          });
        } catch (err) {}
      }`;

code = code.replace(targetMarkerBlock, newMarkerBlock);

// 7. Add simIndex to dependency array of the big useEffect for markers
code = code.replace(/mapLoadedTrigger, interpolatedPath\]\);/g, 'mapLoadedTrigger, interpolatedPath, simIndex]);');

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
