const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');
code = code.replace(/let remainingMiles = 0;.*?const ampm = hours >= 12 \? 'PM' : 'AM';\s*hours = hours % 12;\s*hours = hours \? hours : 12;/s, 
`let totalMiles = 0;
          let coveredMiles = 0;
          for (let i = 0; i < interpolatedPath.length - 1; i++) {
             const dist = haversine(interpolatedPath[i], interpolatedPath[i+1]);
             totalMiles += dist;
             if (i < simIndex) {
               coveredMiles += dist;
             }
          }
          let displayMiles = coveredMiles;
          if (totalSteps === 0) displayMiles = 0;
          
          let startTimeMs = activeTrip.start_time ? new Date(activeTrip.start_time).getTime() : Date.now();
          let endTimeMs = activeTrip.end_time ? new Date(activeTrip.end_time).getTime() : Date.now();
          
          let totalDurationMin = Math.max(1, Math.round((endTimeMs - startTimeMs) / 60000));
          let coveredMin = Math.round(totalDurationMin * (totalSteps > 0 ? (simIndex / Math.max(1, totalSteps - 1)) : 0));
          
          let currentTimeMs = startTimeMs + (coveredMin * 60000);
          const d = new Date(currentTimeMs);
          let hours = d.getHours();
          const minutes = d.getMinutes();
          const ampm = hours >= 12 ? 'PM' : 'AM';
          hours = hours % 12;
          hours = hours ? hours : 12;`);

code = code.replace(/\{remainingMin\} min/, `{coveredMin} min`);
code = code.replace(/\{remainingMiles\.toFixed\(1\)\} mi/, `{displayMiles.toFixed(1)} mi`);

// Also update the max speed logic
code = code.replace(/const \[speedLimit, setSpeedLimit\] = useState<number>\(30\);/, `const [speedLimit, setSpeedLimit] = useState<number>(30);`);
// Wait, speedLimit should be dynamic! We can just define maxSpeed inside the selectedTrip rendering block instead of state!
code = code.replace(/<span className="text-xl font-bold text-slate-900 leading-none">\{speedLimit\}<\/span>/, `<span className="text-xl font-bold text-slate-900 leading-none">{totalDurationMin > 0 ? Math.max(2, Math.round((totalMiles / (totalDurationMin / 60)) * 1.3)) : speedLimit}</span>`);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
