import * as XLSX from "xlsx";

export function downloadExcel(options: { filename: string; sheets: Array<{ name: string; rows: Array<Record<string, unknown>> }> }) {
  const wb = XLSX.utils.book_new();

  for (const sheet of options.sheets) {
    const ws = XLSX.utils.json_to_sheet(sheet.rows);
    XLSX.utils.book_append_sheet(wb, ws, sheet.name.slice(0, 31)); // Excel limit
  }

  XLSX.writeFile(wb, options.filename);
}

