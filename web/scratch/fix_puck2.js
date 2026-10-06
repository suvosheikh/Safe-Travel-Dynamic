const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

// 1. Fix the messed up START POINT MARKER
const brokenStartMarker = `        // --- DRAW START POINT MARKER (Google Maps style) ---
        const startPointCoords = trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords);
        if (startMarkersRef.current[trip.id]) {
            startMarkersRef.current[trip.id].setLngLat(startPointCoords);
          } else {
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
              
              const tooltip = document.createElement('div');el.innerHTML = '<div class="gps-pin-wrapper fallback-size"><div class="gps-map-pin dynamic-pin departure"><span class="material-icons map-pin-icon" style="font-size:12px;margin-top:1px;">trip_origin</span></div></div>';
              
              const marker = new mapboxgl.Marker({ element: el, anchor: 'bottom' })
                .setLngLat(startPointCoords)
                .addTo(mapRef.current);
              startMarkersRef.current[trip.id] = marker;
            } catch (e) {}
        }`;

const correctStartMarker = `        // --- DRAW START POINT MARKER (Google Maps style) ---
        const startPointCoords = trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords);
        if (startMarkersRef.current[trip.id]) {
            startMarkersRef.current[trip.id].setLngLat(startPointCoords);
        } else {
            try {
              const el = document.createElement('div');
              el.className = 'mapbox-departure-pin';
              el.innerHTML = '<div class="gps-pin-wrapper fallback-size"><div class="gps-map-pin dynamic-pin departure"><span class="material-icons map-pin-icon" style="font-size:12px;margin-top:1px;">trip_origin</span></div></div>';
              
              const marker = new mapboxgl.Marker({ element: el, anchor: 'bottom' })
                .setLngLat(startPointCoords)
                .addTo(mapRef.current);
              startMarkersRef.current[trip.id] = marker;
            } catch (e) {}
        }`;

code = code.replace(brokenStartMarker, correctStartMarker);

// 2. Actually add the innerHTML to the TRAVELER MARKER correctly
const targetTravelerMarker = `          } else {
            try {
              const el = document.createElement('div');
              el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
              if (isSelected) el.classList.add('selected-pulse');
              
              const tooltip = document.createElement('div');`;

const correctTravelerMarker = `          } else {
            try {
              const el = document.createElement('div');
              el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
              if (isSelected) el.classList.add('selected-pulse');
              
              const puckInner = document.createElement('div');
              puckInner.className = 'gps-navigation-puck mini';
              puckInner.innerHTML = \`
                <div class="puck-pulse-ring \${isSelected ? 'puck-selected' : ''}"></div>
                <div class="puck-core \${isSelected ? 'selected' : ''}">
                   <div class="puck-rotation" style="transform: rotate(\${heading}deg);">
                     <div class="puck-arrow-delta"></div>
                   </div>
                </div>
              \`;
              el.appendChild(puckInner);
              
              const tooltip = document.createElement('div');`;

code = code.replace(targetTravelerMarker, correctTravelerMarker);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
