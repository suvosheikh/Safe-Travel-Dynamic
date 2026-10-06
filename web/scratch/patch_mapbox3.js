const fs = require('fs');
let code = fs.readFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', 'utf8');

const replacement = `// Calculate rotation heading
        let heading = 0;
        if (isSelected && interpolatedPath.length > 0) {
          const current = interpolatedPath[simIndexRef.current];
          const next = interpolatedPath[simIndexRef.current + 1] || current;
          if (current && next) heading = calculateSimpleHeading(current, next);
        } else {
          heading = getRouteHeading(routeCoords);
        }

        // If marker already exists, update styling
        if (markersRef.current[trip.id]) {
          const m = markersRef.current[trip.id];
          try {
            m.setLngLat(coords);
            const el = m.getElement();
            if (el) {
              const transform = el.style.transform;
              const translateMatch = transform.match(/translate\\([^)]+\\)/);
              const translateStr = translateMatch ? translateMatch[0] : '';
              el.style.transform = \`\${translateStr} rotate(\${heading}deg)\`;
            }
          } catch (e) { }
        } else {
          try {
            const el = document.createElement('div');
            el.className = isSOS ? 'mapbox-sos-marker' : 'mapbox-custom-marker';
            if (isSelected) el.classList.add('selected-pulse');
            
            const tooltip = document.createElement('div');
            tooltip.className = 'marker-tooltip';
            const travelerName = traveler?.full_name || 'Traveler';
            tooltip.innerHTML = \`\${travelerName}<br/><span class="text-[9px]">\${trip.transport_mode || 'Walking'}</span>\`;
            el.appendChild(tooltip);

            const marker = new mapboxgl.Marker({
              element: el,
              anchor: 'center'
            })
            .setLngLat(coords)
            .addTo(mapRef.current);

            markersRef.current[trip.id] = marker;
          } catch (err) {}
        }
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ activeTripsList, profiles, selectedTripId, isTokenConfigured, mapboxToken, onSelectTrip, resolvedCoords, roadGeometries, mapLoadedTrigger, interpolatedPath]);`;

code = code.replace(/\/\/\s*Calculate rotation heading\s*let heading = 0;\s*\}\s*\/\/\s*eslint-disable-next-line react-hooks\/exhaustive-deps\s*\}, \[\s*isSimulating,\s*interpolatedPath,\s*isTokenConfigured,\s*mapboxToken,\s*selectedTripId\]\);/ms, replacement);

fs.writeFileSync('D:/Soft/safe-travel-web/components/MapboxMonitor.tsx', code);
