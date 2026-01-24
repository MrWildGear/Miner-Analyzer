use chrono::{DateTime, NaiveDate, NaiveDateTime, Utc};
use once_cell::sync::Lazy;
use rayon::prelude::*;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::fs::{self, File};
use std::io::{BufRead, BufReader};
use std::path::Path;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LogFileInfo {
    pub name: String,
    #[serde(with = "chrono::serde::ts_seconds")]
    pub mtime: DateTime<Utc>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CharacterMiningData {
    #[serde(rename = "characterName")]
    pub character_name: String,
    #[serde(rename = "totalMined")]
    pub total_mined: u64,
    #[serde(rename = "critMined")]
    pub crit_mined: u64,
    #[serde(rename = "totalCycles")]
    pub total_cycles: u64,
    #[serde(rename = "critCycles")]
    pub crit_cycles: u64,
    #[serde(rename = "totalResidue")]
    pub total_residue: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OreData {
    #[serde(rename = "oreName")]
    pub ore_name: String,
    #[serde(rename = "nonCrit")]
    pub non_crit: u64,
    pub crit: u64,
    pub residue: u64,
}

// CharacterOreBreakdown is a nested HashMap: Record<string, Record<string, OreAmounts>>
pub type CharacterOreBreakdown = HashMap<String, HashMap<String, OreAmounts>>;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OreAmounts {
    #[serde(rename = "nonCrit")]
    pub non_crit: u64,
    pub crit: u64,
    pub residue: u64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DateRange {
    pub start: Option<String>, // ISO 8601 string
    pub end: Option<String>,   // ISO 8601 string
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OverallStats {
    #[serde(rename = "totalMined")]
    pub total_mined: u64,
    #[serde(rename = "totalCrit")]
    pub total_crit: u64,
    #[serde(rename = "totalCycles")]
    pub total_cycles: u64,
    #[serde(rename = "totalCritCycles")]
    pub total_crit_cycles: u64,
    #[serde(rename = "totalResidue")]
    pub total_residue: u64,
    #[serde(rename = "overallCritRate")]
    pub overall_crit_rate: f64,
    #[serde(rename = "overallResidueRate")]
    pub overall_residue_rate: f64,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LogAnalysisResult {
    pub characters: HashMap<String, CharacterMiningData>,
    #[serde(rename = "oreBreakdown")]
    pub ore_breakdown: HashMap<String, OreData>,
    #[serde(rename = "characterOreBreakdown")]
    pub character_ore_breakdown: CharacterOreBreakdown,
    #[serde(rename = "dateRange")]
    pub date_range: DateRangeResult,
    #[serde(rename = "activeDays")]
    pub active_days: Vec<String>, // ISO date strings
    pub overall: OverallStats,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DateRangeResult {
    pub start: String, // ISO date string
    pub end: String,  // ISO date string
}

// Compile regex patterns once

static LOG_FILE_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(r"^\d{8}_\d{6}(_\d+)?\.txt$").unwrap());
static LISTENER_PATTERN: Lazy<Regex> = Lazy::new(|| Regex::new(r"Listener:\s*(.+)").unwrap());
static REGULAR_MINING_PATTERN: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"\(mining\).*?You mined.*?<color=#ff8dc169>(\d+)<.*?units of.*?<font size=12>([^<\r\n]+)").unwrap()
});
static CRIT_MINING_PATTERN: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"\(mining\).*?Critical mining success!.*?You mined an additional.*?<color=#fff0ff45><font size=12>(\d+)<.*?units of.*?<font size=12>([^<\r\n]+)").unwrap()
});
static RESIDUE_PATTERN: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"\(mining\).*?Additional\s+<font size=12><color=#ffff454b>(\d+)<.*?units depleted from asteroid as residue").unwrap()
});
static REGULAR_MINING_LINE_PATTERN: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"\(mining\).*?You mined.*?<color=#ff8dc169>\d+<.*?units of.*?<font size=12>([^<\r\n]+)").unwrap()
});
static CRIT_MINING_LINE_PATTERN: Lazy<Regex> = Lazy::new(|| {
    Regex::new(r"\(mining\).*?Critical mining success!.*?You mined an additional.*?<color=#fff0ff45><font size=12>\d+<.*?units of.*?<font size=12>([^<\r\n]+)").unwrap()
});

pub fn find_log_files(directory: &str) -> Result<Vec<LogFileInfo>, String> {
    let dir = Path::new(directory);
    let entries = fs::read_dir(dir)
        .map_err(|e| format!("Error reading directory: {}", e))?;

    let mut log_files = Vec::new();

    for entry in entries {
        let entry = entry.map_err(|e| format!("Error reading directory entry: {}", e))?;
        let file_name = entry.file_name();
        let name = file_name.to_string_lossy();

        if LOG_FILE_PATTERN.is_match(&name) {
            let file_path = entry.path();
            match fs::metadata(&file_path) {
                Ok(metadata) => {
                    if let Ok(mtime) = metadata.modified() {
                        let mtime_dt: DateTime<Utc> = mtime.into();
                        log_files.push(LogFileInfo {
                            name: name.to_string(),
                            mtime: mtime_dt,
                        });
                    } else {
                        // Fallback to current time
                        log_files.push(LogFileInfo {
                            name: name.to_string(),
                            mtime: Utc::now(),
                        });
                    }
                }
                Err(_) => {
                    // Still include the file with current date as fallback
                    log_files.push(LogFileInfo {
                        name: name.to_string(),
                        mtime: Utc::now(),
                    });
                }
            }
        }
    }

    // Sort by modification date descending (most recent first)
    log_files.sort_by(|a, b| b.mtime.cmp(&a.mtime));

    Ok(log_files)
}

pub fn extract_character_name(file_path: &str) -> Result<Option<String>, String> {
    let file = File::open(file_path)
        .map_err(|e| format!("Error opening file {}: {}", file_path, e))?;
    let reader = BufReader::new(file);

    // Read only first 5 lines instead of entire file
    for line in reader.lines().take(5) {
        let line = line.map_err(|e| format!("Error reading line: {}", e))?;
        if let Some(captures) = LISTENER_PATTERN.captures(&line) {
            if let Some(char_name) = captures.get(1) {
                return Ok(Some(char_name.as_str().trim().to_string()));
            }
        }
    }

    Ok(None)
}

fn normalize_to_start_of_day(dt: DateTime<Utc>) -> NaiveDate {
    dt.date_naive()
}

/// Extract character names from multiple files in parallel
fn extract_character_names_parallel(
    directory: &str,
    file_names: &[&str],
) -> Result<HashMap<String, Option<String>>, String> {
    let results: Result<Vec<(String, Option<String>)>, String> = file_names
        .par_iter()
        .map(|file_name| {
            let file_path = format!("{}/{}", directory, file_name);
            let char_name = extract_character_name(&file_path)?;
            Ok((file_name.to_string(), char_name))
        })
        .collect();

    Ok(results?.into_iter().collect())
}

pub fn identify_session_characters(
    log_files: &[LogFileInfo],
    date_range: &Option<DateRange>,
    character_cache: &HashMap<String, Option<String>>,
) -> Result<HashSet<String>, String> {
    if log_files.is_empty() {
        return Ok(HashSet::new());
    }

    let files_to_check: Vec<&LogFileInfo> = if let Some(range) = date_range {
        if range.start.is_some() || range.end.is_some() {
            let range_start = range
                .start
                .as_ref()
                .and_then(|s| DateTime::parse_from_rfc3339(s).ok())
                .map(|dt| normalize_to_start_of_day(dt.with_timezone(&Utc)));
            let range_end = range
                .end
                .as_ref()
                .and_then(|s| DateTime::parse_from_rfc3339(s).ok())
                .map(|dt| normalize_to_start_of_day(dt.with_timezone(&Utc)));

            log_files
                .iter()
                .filter(|file| {
                    let file_date = normalize_to_start_of_day(file.mtime);
                    match (range_start, range_end) {
                        (Some(start), Some(end)) => file_date >= start && file_date <= end,
                        (Some(start), None) => file_date >= start,
                        (None, Some(end)) => file_date <= end,
                        (None, None) => true,
                    }
                })
                .collect()
        } else {
            log_files.iter().collect()
        }
    } else {
        // No date range: use most recent hour
        let most_recent_file = &log_files[0];
        let session_hour_prefix = &most_recent_file.name[..11]; // YYYYMMDD_HH
        log_files
            .iter()
            .filter(|file| file.name.starts_with(session_hour_prefix))
            .collect()
    };

    let mut session_characters = HashSet::new();

    for file in files_to_check {
        if let Some(Some(char_name)) = character_cache.get(&file.name) {
            session_characters.insert(char_name.clone());
        }
    }

    Ok(session_characters)
}

pub fn get_files_for_date_range(
    log_files: &[LogFileInfo],
    session_characters: &HashSet<String>,
    date_range: &Option<DateRange>,
    character_cache: &HashMap<String, Option<String>>,
) -> Result<Vec<String>, String> {
    let mut filtered_files = Vec::new();

    // Normalize date range
    let range_start = date_range
        .as_ref()
        .and_then(|r| r.start.as_ref())
        .and_then(|s| DateTime::parse_from_rfc3339(s).ok())
        .map(|dt| normalize_to_start_of_day(dt.with_timezone(&Utc)));

    let range_end = date_range
        .as_ref()
        .and_then(|r| r.end.as_ref())
        .and_then(|s| DateTime::parse_from_rfc3339(s).ok())
        .map(|dt| normalize_to_start_of_day(dt.with_timezone(&Utc)));

    for file in log_files {
        // Filter by date range
        if let (Some(start), Some(end)) = (range_start, range_end) {
            let file_date = normalize_to_start_of_day(file.mtime);
            if file_date < start || file_date > end {
                continue;
            }
        } else if let Some(start) = range_start {
            let file_date = normalize_to_start_of_day(file.mtime);
            if file_date < start {
                continue;
            }
        } else if let Some(end) = range_end {
            let file_date = normalize_to_start_of_day(file.mtime);
            if file_date > end {
                continue;
            }
        }

        // Check if file belongs to session characters using cache
        if let Some(Some(char_name)) = character_cache.get(&file.name) {
            if session_characters.contains(char_name) {
                filtered_files.push(file.name.clone());
            }
        }
    }

    Ok(filtered_files)
}

fn ensure_char_ore<'a>(
    character_ore_breakdown: &'a mut CharacterOreBreakdown,
    character_name: &str,
    ore_name: &str,
) -> &'a mut OreAmounts {
    let char_map = character_ore_breakdown
        .entry(character_name.to_string())
        .or_insert_with(HashMap::new);
    char_map
        .entry(ore_name.to_string())
        .or_insert_with(|| OreAmounts {
            non_crit: 0,
            crit: 0,
            residue: 0,
        })
}

pub fn parse_log_file(
    directory: &str,
    file_name: &str,
    character_data: &mut HashMap<String, CharacterMiningData>,
    ore_data: &mut HashMap<String, OreData>,
    character_ore_breakdown: &mut CharacterOreBreakdown,
) -> Result<(), String> {
    let file_path = format!("{}/{}", directory, file_name);
    let content = fs::read_to_string(&file_path)
        .map_err(|e| format!("Error reading file {}: {}", file_path, e))?;

    // Extract character name
    let listener_match = LISTENER_PATTERN
        .captures(&content)
        .and_then(|c| c.get(1))
        .map(|m| m.as_str().trim().to_string());

    let character_name = match listener_match {
        Some(name) => name,
        None => {
            return Err(format!("Could not find character name in {}", file_name));
        }
    };

    // Initialize character data if not exists
    character_data
        .entry(character_name.clone())
        .or_insert_with(|| CharacterMiningData {
            character_name: character_name.clone(),
            total_mined: 0,
            crit_mined: 0,
            total_cycles: 0,
            crit_cycles: 0,
            total_residue: 0,
        });

    let char_data = character_data.get_mut(&character_name).unwrap();

    // Parse regular mining
    for cap in REGULAR_MINING_PATTERN.captures_iter(&content) {
        if let (Some(amount_str), Some(ore_name_match)) = (cap.get(1), cap.get(2)) {
            let amount: u64 = amount_str
                .as_str()
                .parse()
                .map_err(|e| format!("Error parsing amount: {}", e))?;
            let ore_name = ore_name_match.as_str().trim().to_string();

            char_data.total_mined += amount;
            char_data.total_cycles += 1;

            // Track ore data
            let ore = ore_data
                .entry(ore_name.clone())
                .or_insert_with(|| OreData {
                    ore_name: ore_name.clone(),
                    non_crit: 0,
                    crit: 0,
                    residue: 0,
                });
            ore.non_crit += amount;

            let char_ore = ensure_char_ore(character_ore_breakdown, &character_name, &ore_name);
            char_ore.non_crit += amount;
        }
    }

    // Parse critical mining
    for cap in CRIT_MINING_PATTERN.captures_iter(&content) {
        if let (Some(amount_str), Some(ore_name_match)) = (cap.get(1), cap.get(2)) {
            let amount: u64 = amount_str
                .as_str()
                .parse()
                .map_err(|e| format!("Error parsing amount: {}", e))?;
            let ore_name = ore_name_match.as_str().trim().to_string();

            char_data.crit_mined += amount;
            char_data.crit_cycles += 1;

            // Track ore data
            let ore = ore_data
                .entry(ore_name.clone())
                .or_insert_with(|| OreData {
                    ore_name: ore_name.clone(),
                    non_crit: 0,
                    crit: 0,
                    residue: 0,
                });
            ore.crit += amount;

            let char_ore = ensure_char_ore(character_ore_breakdown, &character_name, &ore_name);
            char_ore.crit += amount;
        }
    }

    // Parse residue - need to track last mined ore
    let lines: Vec<&str> = content.lines().collect();
    let mut last_mined_ore: Option<String> = None;

    for line in lines {
        // Track last mined ore from regular mining
        if let Some(cap) = REGULAR_MINING_LINE_PATTERN.captures(line) {
            if let Some(ore_match) = cap.get(1) {
                last_mined_ore = Some(ore_match.as_str().trim().to_string());
            }
        }

        // Also track from crit mining
        if let Some(cap) = CRIT_MINING_LINE_PATTERN.captures(line) {
            if let Some(ore_match) = cap.get(1) {
                last_mined_ore = Some(ore_match.as_str().trim().to_string());
            }
        }

        // Check for residue line
        if let Some(cap) = RESIDUE_PATTERN.captures(line) {
            if let (Some(amount_str), Some(ore)) = (cap.get(1), &last_mined_ore) {
                let amount: u64 = amount_str
                    .as_str()
                    .parse()
                    .map_err(|e| format!("Error parsing residue amount: {}", e))?;

                char_data.total_residue += amount;

                // Attribute residue to last mined ore
                let ore_entry = ore_data
                    .entry(ore.clone())
                    .or_insert_with(|| OreData {
                        ore_name: ore.clone(),
                        non_crit: 0,
                        crit: 0,
                        residue: 0,
                    });
                ore_entry.residue += amount;

                let char_ore = ensure_char_ore(character_ore_breakdown, &character_name, ore);
                char_ore.residue += amount;
            }
        }
    }

    Ok(())
}

pub fn get_active_days(log_files: &[LogFileInfo]) -> Vec<NaiveDate> {
    let mut active_days_set = HashSet::new();

    for file in log_files {
        let file_date = normalize_to_start_of_day(file.mtime);
        active_days_set.insert(file_date);
    }

    let mut active_days: Vec<NaiveDate> = active_days_set.into_iter().collect();
    active_days.sort();
    active_days
}

pub fn analyze_logs(
    directory: String,
    date_range: Option<DateRange>,
) -> Result<Option<LogAnalysisResult>, String> {
    // Find all log files
    let all_log_files = find_log_files(&directory)?;
    if all_log_files.is_empty() {
        return Ok(None);
    }

    // Extract all character names in one parallel pass
    let file_names: Vec<&str> = all_log_files.iter().map(|f| f.name.as_str()).collect();
    let character_cache = extract_character_names_parallel(&directory, &file_names)?;

    // Identify session characters using cache
    let session_characters = identify_session_characters(&all_log_files, &date_range, &character_cache)?;
    if session_characters.is_empty() {
        return Ok(None);
    }

    // Get files for date range using cache
    let files_to_process =
        get_files_for_date_range(&all_log_files, &session_characters, &date_range, &character_cache)?;

    if files_to_process.is_empty() {
        return Ok(None);
    }

    // Parse all files in parallel
    let parse_results: Result<Vec<_>, String> = files_to_process
        .par_iter()
        .map(|file_name| {
            let mut character_data: HashMap<String, CharacterMiningData> = HashMap::new();
            let mut ore_data: HashMap<String, OreData> = HashMap::new();
            let mut character_ore_breakdown: CharacterOreBreakdown = HashMap::new();

            parse_log_file(
                &directory,
                file_name,
                &mut character_data,
                &mut ore_data,
                &mut character_ore_breakdown,
            )?;

            Ok((character_data, ore_data, character_ore_breakdown))
        })
        .collect();

    // Merge all results
    let mut character_data: HashMap<String, CharacterMiningData> = HashMap::new();
    let mut ore_data: HashMap<String, OreData> = HashMap::new();
    let mut character_ore_breakdown: CharacterOreBreakdown = HashMap::new();

    for (mut char_data, mut ore_data_part, mut char_ore_part) in parse_results? {
        // Merge character data
        for (char_name, char_info) in char_data.drain() {
            let entry = character_data.entry(char_name.clone()).or_insert_with(|| CharacterMiningData {
                character_name: char_name.clone(),
                total_mined: 0,
                crit_mined: 0,
                total_cycles: 0,
                crit_cycles: 0,
                total_residue: 0,
            });
            entry.total_mined += char_info.total_mined;
            entry.crit_mined += char_info.crit_mined;
            entry.total_cycles += char_info.total_cycles;
            entry.crit_cycles += char_info.crit_cycles;
            entry.total_residue += char_info.total_residue;
        }

        // Merge ore data
        for (ore_name, ore_info) in ore_data_part.drain() {
            let entry = ore_data.entry(ore_name.clone()).or_insert_with(|| OreData {
                ore_name: ore_name.clone(),
                non_crit: 0,
                crit: 0,
                residue: 0,
            });
            entry.non_crit += ore_info.non_crit;
            entry.crit += ore_info.crit;
            entry.residue += ore_info.residue;
        }

        // Merge character ore breakdown
        for (char_name, char_ores) in char_ore_part.drain() {
            let char_entry = character_ore_breakdown.entry(char_name).or_insert_with(HashMap::new);
            for (ore_name, ore_amounts) in char_ores {
                let ore_entry = char_entry.entry(ore_name).or_insert_with(|| OreAmounts {
                    non_crit: 0,
                    crit: 0,
                    residue: 0,
                });
                ore_entry.non_crit += ore_amounts.non_crit;
                ore_entry.crit += ore_amounts.crit;
                ore_entry.residue += ore_amounts.residue;
            }
        }
    }

    // Calculate overall statistics
    let mut total_mined = 0u64;
    let mut total_crit = 0u64;
    let mut total_cycles = 0u64;
    let mut total_crit_cycles = 0u64;
    let mut total_residue = 0u64;

    for char in character_data.values() {
        total_mined += char.total_mined;
        total_crit += char.crit_mined;
        total_cycles += char.total_cycles;
        total_crit_cycles += char.crit_cycles;
        total_residue += char.total_residue;
    }

    let overall_crit_rate = if total_cycles > 0 {
        (total_crit_cycles as f64 / total_cycles as f64) * 100.0
    } else {
        0.0
    };

    let total_potential = total_mined + total_crit + total_residue;
    let overall_residue_rate = if total_potential > 0 {
        (total_residue as f64 / total_potential as f64) * 100.0
    } else {
        0.0
    };

    // Get active days from processed files
    let processed_file_infos: Vec<LogFileInfo> = all_log_files
        .into_iter()
        .filter(|file| files_to_process.contains(&file.name))
        .collect();
    let active_days = get_active_days(&processed_file_infos);

    // Determine date range from files if not provided or partially provided
    let final_date_range = if let Some(range) = &date_range {
        if range.start.is_some() && range.end.is_some() {
            DateRangeResult {
                start: range.start.as_ref().unwrap().clone(),
                end: range.end.as_ref().unwrap().clone(),
            }
        } else if range.start.is_some() {
            let end_date = active_days
                .last()
                .map(|d| {
                    let dt = NaiveDateTime::new(*d, chrono::NaiveTime::from_hms_opt(23, 59, 59).unwrap());
                    DateTime::<Utc>::from_naive_utc_and_offset(dt, Utc).to_rfc3339()
                })
                .unwrap_or_else(|| range.start.as_ref().unwrap().clone());
            DateRangeResult {
                start: range.start.as_ref().unwrap().clone(),
                end: end_date,
            }
        } else if range.end.is_some() {
            let start_date = active_days
                .first()
                .map(|d| {
                    let dt = NaiveDateTime::new(*d, chrono::NaiveTime::from_hms_opt(0, 0, 0).unwrap());
                    DateTime::<Utc>::from_naive_utc_and_offset(dt, Utc).to_rfc3339()
                })
                .unwrap_or_else(|| range.end.as_ref().unwrap().clone());
            DateRangeResult {
                start: start_date,
                end: range.end.as_ref().unwrap().clone(),
            }
        } else if !active_days.is_empty() {
            let start_dt = NaiveDateTime::new(active_days[0], chrono::NaiveTime::from_hms_opt(0, 0, 0).unwrap());
            let end_dt = NaiveDateTime::new(active_days[active_days.len() - 1], chrono::NaiveTime::from_hms_opt(23, 59, 59).unwrap());
            DateRangeResult {
                start: DateTime::<Utc>::from_naive_utc_and_offset(start_dt, Utc).to_rfc3339(),
                end: DateTime::<Utc>::from_naive_utc_and_offset(end_dt, Utc).to_rfc3339(),
            }
        } else {
            let today = Utc::now().to_rfc3339();
            DateRangeResult {
                start: today.clone(),
                end: today,
            }
        }
    } else if !active_days.is_empty() {
        let start_dt = NaiveDateTime::new(active_days[0], chrono::NaiveTime::from_hms_opt(0, 0, 0).unwrap());
        let end_dt = NaiveDateTime::new(active_days[active_days.len() - 1], chrono::NaiveTime::from_hms_opt(23, 59, 59).unwrap());
        DateRangeResult {
            start: DateTime::<Utc>::from_naive_utc_and_offset(start_dt, Utc).to_rfc3339(),
            end: DateTime::<Utc>::from_naive_utc_and_offset(end_dt, Utc).to_rfc3339(),
        }
    } else {
        let today = Utc::now().to_rfc3339();
        DateRangeResult {
            start: today.clone(),
            end: today,
        }
    };

    Ok(Some(LogAnalysisResult {
        characters: character_data,
        ore_breakdown: ore_data,
        character_ore_breakdown,
        date_range: final_date_range,
        active_days: active_days
            .iter()
            .map(|d| {
                let dt = NaiveDateTime::new(*d, chrono::NaiveTime::from_hms_opt(0, 0, 0).unwrap());
                DateTime::<Utc>::from_naive_utc_and_offset(dt, Utc).to_rfc3339()
            })
            .collect(),
        overall: OverallStats {
            total_mined,
            total_crit,
            total_cycles,
            total_crit_cycles,
            total_residue,
            overall_crit_rate,
            overall_residue_rate,
        },
    }))
}
