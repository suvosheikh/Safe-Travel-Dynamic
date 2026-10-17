/**
 * Utility for formatting date & time strictly in GMT+06 (Asia/Dhaka).
 * Target pattern: "10 Oct 2026 10:30 AM"
 */

export function formatGmtPlus6DateTime(dateInput?: string | Date | number | null): string {
  if (!dateInput) return "";
  const d = typeof dateInput === "string" || typeof dateInput === "number" ? new Date(dateInput) : dateInput;
  if (isNaN(d.getTime())) return String(dateInput);

  try {
    const formatter = new Intl.DateTimeFormat("en-GB", {
      timeZone: "Asia/Dhaka", // GMT+06:00
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: true,
    });

    const parts = formatter.formatToParts(d);
    let day = "";
    let month = "";
    let year = "";
    let hour = "";
    let minute = "";
    let dayPeriod = "";

    for (const part of parts) {
      if (part.type === "day") day = part.value;
      else if (part.type === "month") month = part.value;
      else if (part.type === "year") year = part.value;
      else if (part.type === "hour") hour = part.value;
      else if (part.type === "minute") minute = part.value;
      else if (part.type === "dayPeriod") dayPeriod = part.value.toUpperCase();
    }

    return `${day} ${month} ${year} ${hour}:${minute} ${dayPeriod}`.trim();
  } catch (_e) {
    return d.toLocaleString("en-US", { timeZone: "Asia/Dhaka" });
  }
}

export function formatGmtPlus6Date(dateInput?: string | Date | number | null): string {
  if (!dateInput) return "";
  const d = typeof dateInput === "string" || typeof dateInput === "number" ? new Date(dateInput) : dateInput;
  if (isNaN(d.getTime())) return String(dateInput);

  try {
    const formatter = new Intl.DateTimeFormat("en-GB", {
      timeZone: "Asia/Dhaka",
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
    const parts = formatter.formatToParts(d);
    let day = "";
    let month = "";
    let year = "";
    for (const part of parts) {
      if (part.type === "day") day = part.value;
      else if (part.type === "month") month = part.value;
      else if (part.type === "year") year = part.value;
    }
    return `${day} ${month} ${year}`.trim();
  } catch (_e) {
    return d.toLocaleDateString();
  }
}
