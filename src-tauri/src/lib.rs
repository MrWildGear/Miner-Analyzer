mod log_parser;

use log_parser::{analyze_logs, find_log_files, DateRange, LogAnalysisResult, LogFileInfo};

#[tauri::command]
fn find_log_files_command(directory: String) -> Result<Vec<LogFileInfo>, String> {
    find_log_files(&directory)
}

#[tauri::command]
fn analyze_logs_command(
    directory: String,
    date_range: Option<DateRange>,
) -> Result<Option<LogAnalysisResult>, String> {
    analyze_logs(directory, date_range)
}

#[tauri::command]
fn get_active_days_command(directory: String) -> Result<Vec<String>, String> {
    use chrono::{NaiveDateTime, Utc};
    let log_files = find_log_files(&directory)?;
    let active_days = log_parser::get_active_days(&log_files);
    Ok(active_days
        .iter()
        .map(|d| {
            let dt = NaiveDateTime::new(*d, chrono::NaiveTime::from_hms_opt(0, 0, 0).unwrap());
            chrono::DateTime::<Utc>::from_naive_utc_and_offset(dt, Utc).to_rfc3339()
        })
        .collect())
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .plugin(tauri_plugin_clipboard_manager::init())
        .plugin(tauri_plugin_dialog::init())
        .plugin(tauri_plugin_fs::init())
        .plugin(tauri_plugin_shell::init())
        .invoke_handler(tauri::generate_handler![
            find_log_files_command,
            analyze_logs_command,
            get_active_days_command
        ])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
