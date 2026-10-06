const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

// 1. Fix missing markers array definitions
code = code.replace(/const destMarkersRef = useRef/, 'const startMarkersRef = useRef<{ [key: string]: any }>({});\n  const destMarkersRef = useRef');
code = code.replace(/destMarkersRef\.current = \{\};/, 'startMarkersRef.current = {};\n      destMarkersRef.current = {};');

// 2. Add cleanup for startMarkersRef
const cleanupDestRegex = /\/\/ Remove target destination markers that are no longer active[\s\S]*?delete destMarkersRef\.current\[id\];\s*\}\s*\}\);/m;
code = code.replace(cleanupDestRegex, `// Remove target destination markers that are no longer active
    Object.keys(destMarkersRef.current).forEach(id => {
      if (!activeIds.has(id)) {
        try { destMarkersRef.current[id].remove(); } catch {}
        delete destMarkersRef.current[id];
      }
    });
    Object.keys(startMarkersRef.current).forEach(id => {
      if (!activeIds.has(id)) {
        try { startMarkersRef.current[id].remove(); } catch {}
        delete startMarkersRef.current[id];
      }
    });`);

// 3. Inject Start and End marker creation in the activeTripsList.forEach loop
// Look for where the main traveler marker is created (the end of the loop)
const markerCreationEnd = `          } catch (err) {}
        }
      });`;

const newMarkerCreationEnd = `          } catch (err) {}
        }

        // --- DRAW START POINT MARKER (Google Maps style) ---
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
        }

        // --- DRAW END POINT MARKER (Google Maps style) ---
        const isUnknownDest = !trip.end_location || trip.end_location.includes('Unknown Destination');
        if (!isUnknownDest) {
            if (destMarkersRef.current[trip.id]) {
                destMarkersRef.current[trip.id].setLngLat(endCoords);
            } else {
                try {
                  const el = document.createElement('div');
                  el.className = 'mapbox-destination-pin';
                  el.innerHTML = '<div class="gps-pin-wrapper fallback-size"><div class="gps-map-pin red-pin"><span class="material-icons map-pin-icon" style="font-size:12px;margin-top:1px;">location_on</span></div></div>';
                  
                  const marker = new mapboxgl.Marker({ element: el, anchor: 'bottom' })
                    .setLngLat(endCoords)
                    .addTo(mapRef.current);
                  destMarkersRef.current[trip.id] = marker;
                } catch (e) {}
            }
        }

      });`;

code = code.replace(markerCreationEnd, newMarkerCreationEnd);

// 4. Safely wrap the route adding in a try/catch and ensure rawCoords has at least 2 points
const updateRoutesRegex = /const updateRoutes = \(\) => \{[\s\S]*?activeTripsList\.forEach\(\(trip\) => \{[\s\S]*?const isSelected = trip\.id === selectedTripId;[\s\S]*?const rawCoords = extractRouteLog[^;]+;/m;

const safeUpdateRoutes = `const updateRoutes = () => {
      if (!mapInstance.isStyleLoaded()) {
        setTimeout(updateRoutes, 200);
        return;
      }
      
      activeTripsList.forEach((trip) => {
        const isSelected = trip.id === selectedTripId;
        const sourceId = \`route-source-\${trip.id}\`;
        const layerId = \`route-layer-\${trip.id}\`;
        const shadowLayerId = \`route-shadow-\${trip.id}\`;

        const endCoords = (trip.end_coords || getCoordsSync(trip.end_location, resolvedCoords));
        const key = \`\${trip.id}-\${trip.start_location}-\${endCoords[0].toFixed(4)},\${endCoords[1].toFixed(4)}\`;
        
        let rawCoords = extractRouteLog(trip.route_path_log) || roadGeometries[key] || generateSimulatedRoadRoute(
          (trip.start_coords || getCoordsSync(trip.start_location, resolvedCoords)), 
          endCoords
        );

        if (!rawCoords || rawCoords.length < 2) return; // Mapbox will crash if we pass an invalid array
`;

code = code.replace(/const updateRoutes = \(\) => \{[\s\S]*?const rawCoords = extractRouteLog[^;]+;/m, safeUpdateRoutes);

// Ensure mapInstance.addSource is wrapped in try/catch just in case!
const sourceAddingBlockRegex = /if \(\!mapInstance\.getSource\(sourceId\)\) \{[\s\S]*?\}\s*\}\s*\n\s*\}\s*\n\s*\}\s*\n\s*\}\);/m;

const safeSourceAddingBlock = `if (!mapInstance.getSource(sourceId)) {
          try {
            mapInstance.addSource(sourceId, {
              type: 'geojson',
              data: { type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: rawCoords } }
            });

            mapInstance.addLayer({
              id: shadowLayerId,
              type: 'line',
              source: sourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: { 'line-color': '#0f172a', 'line-width': isSelected ? 10 : 6, 'line-opacity': 0.6, 'line-blur': 4 }
            });

            mapInstance.addLayer({
              id: layerId,
              type: 'line',
              source: sourceId,
              layout: { 'line-join': 'round', 'line-cap': 'round' },
              paint: { 'line-color': isSelected ? '#3b82f6' : '#14b8a6', 'line-width': isSelected ? 6 : 3, 'line-opacity': isSelected ? 1 : 0.6 }
            });
          } catch (e) {
            console.error('Failed to add Mapbox route layer', e);
          }
        } else {
          try {
            const sourceObj = mapInstance.getSource(sourceId);
            if (sourceObj) {
              sourceObj.setData({ type: 'Feature', properties: {}, geometry: { type: 'LineString', coordinates: rawCoords } });
            }
            if (mapInstance.getLayer(layerId)) {
              mapInstance.setPaintProperty(layerId, 'line-color', isSelected ? '#3b82f6' : '#14b8a6');
              mapInstance.setPaintProperty(layerId, 'line-width', isSelected ? 6 : 3);
              mapInstance.setPaintProperty(layerId, 'line-opacity', isSelected ? 1 : 0.6);
            }
            if (mapInstance.getLayer(shadowLayerId)) {
              mapInstance.setPaintProperty(shadowLayerId, 'line-width', isSelected ? 10 : 6);
            }
          } catch (e) {
            console.error('Failed to update Mapbox route layer', e);
          }
        }
      });`;

code = code.replace(sourceAddingBlockRegex, safeSourceAddingBlock);


fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
