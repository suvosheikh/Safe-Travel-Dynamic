const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/scratch/tmp_mapbox.tsx', 'utf8');

// Replace the setInterval block first!
const rAFEffect = `
  const animationRef = useRef<number>(null);
  
  // 2. High-Performance 60 FPS requestAnimationFrame Simulation Loop
  useEffect(() => {
    if (!isSimulating || interpolatedPath.length === 0) return;
    
    let lastTime = performance.now();
    
    const animate = (time: number) => {
      const deltaTime = time - lastTime;
      // Normal speed: 1 step every 15ms (for 1500 steps ~ 22 seconds trip)
      const stepsToAdvance = (deltaTime / 15) * simSpeed;
      
      if (stepsToAdvance >= 1) {
        lastTime = time;
        simIndexRef.current += Math.floor(stepsToAdvance);
        
        if (simIndexRef.current >= interpolatedPath.length - 1) {
          simIndexRef.current = 0; // Seamless loop to demo forever
        }
        
        const index = simIndexRef.current;
        const currentCoords = interpolatedPath[index];
        const nextCoords = interpolatedPath[index + 1] || currentCoords;
        const heading = calculateSimpleHeading(currentCoords, nextCoords);
        
        // 1. Update Marker Directly (Bypassing React state)
        if (selectedTripId && markersRef.current[selectedTripId]) {
          const m = markersRef.current[selectedTripId];
          m.setLngLat(currentCoords);
          const el = m.getElement();
          if (el) el.style.transform = \`\${el.style.transform} rotate(\${heading}deg)\`;
        }
        
        // 2. Smoothly Update Map Camera (Jump immediately instead of conflicting eases)
        if (mapRef.current) {
          mapRef.current.jumpTo({
            center: currentCoords,
            bearing: heading,
            pitch: 52
          });
        }
        
        // 3. Update HUD Directly (Bypassing React re-renders)
        const activeTrip = activeTripsList.find(t => t.id === selectedTripId);
        if (activeTrip) {
          let totalMiles = 0;
          let coveredMiles = 0;
          for (let i = 0; i < interpolatedPath.length - 1; i++) {
             const dist = haversine(interpolatedPath[i], interpolatedPath[i+1]);
             totalMiles += dist;
             if (i < index) coveredMiles += dist;
          }
          
          let startTimeMs = activeTrip.start_time ? new Date(activeTrip.start_time).getTime() : Date.now();
          let endTimeMs = activeTrip.end_time ? new Date(activeTrip.end_time).getTime() : Date.now();
          let totalDurationMin = Math.max(1, Math.round((endTimeMs - startTimeMs) / 60000));
          let coveredMin = Math.round(totalDurationMin * (index / Math.max(1, interpolatedPath.length - 1)));
          
          let currentTimeMs = startTimeMs + (coveredMin * 60000);
          const d = new Date(currentTimeMs);
          let hours = d.getHours();
          const minutes = d.getMinutes();
          const ampm = hours >= 12 ? 'PM' : 'AM';
          hours = hours % 12;
          hours = hours ? hours : 12;
          const minStr = minutes.toString().padStart(2, '0');
          
          const hudMin = document.getElementById('hud-min-display');
          if (hudMin) hudMin.innerText = \`\${coveredMin} min\`;
          
          const hudMi = document.getElementById('hud-mi-display');
          if (hudMi) hudMi.innerText = \`\${coveredMiles.toFixed(1)} mi\`;
          
          const hudTime = document.getElementById('hud-time-display');
          if (hudTime) hudTime.innerText = \`\${hours}:\${minStr} \${ampm}\`;
        }
      }
      
      animationRef.current = requestAnimationFrame(animate);
    };
    
    animationRef.current = requestAnimationFrame(animate);
    
    return () => {
      if (animationRef.current) cancelAnimationFrame(animationRef.current);
    };
  }, [isSimulating, interpolatedPath, simSpeed, selectedTripId, activeTripsList]);
`;

code = code.replace(/useEffect\(\(\) => \{\s*if \(!isSimulating[\s\S]*?clearInterval\(timer\);\s*\}, \[isSimulating, interpolatedPath, simSpeed\]\);/, rAFEffect);

// Replace remaining 'simIndex' usages outside the effect
code = code.replace(/interpolatedPath\[simIndex\]/g, 'interpolatedPath[simIndexRef.current]');
code = code.replace(/simIndex \+ 1/g, 'simIndexRef.current + 1');
code = code.replace(/simIndex,/g, '');
code = code.replace(/i < simIndex/g, 'i < simIndexRef.current');
code = code.replace(/simIndex \//g, 'simIndexRef.current /');

// And we must REMOVE the mapRef.current.easeTo() that was fighting it! 
// Actually, I can just replace the easing block in the map useEffect.
// Since rAF handles it now, we don't need the React state to ease the camera!
const easeRegex = /if \(isSimActive\) \{[\s\S]*?mapRef\.current\.easeTo\(\{[\s\S]*?essential: true[\s\S]*?\}\);[\s\S]*?\} catch \(err\) \{[\s\S]*?\}/g;
code = code.replace(easeRegex, ''); // remove the old easeTo calls

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
