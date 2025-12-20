#!/usr/bin/env python3
"""
EVE Online Strip Miner Roll Analyzer
Reads item stats from clipboard and calculates m3/sec with mutation ranges
Supports both ORE Strip Miner and Modulated Strip Miner II
"""

import tkinter as tk
from tkinter import ttk, scrolledtext
import pyperclip
import re
import math
from datetime import datetime
import threading
import time

# ============================================================================
# BASE STATS AND CONFIGURATION
# ============================================================================

# Base stats for ORE Strip Miner
ORE_BASE_STATS = {
    'ActivationCost': 23.0,
    'StructureHitpoints': 40,
    'Volume': 5,
    'OptimalRange': 18.75,
    'ActivationTime': 45.0,
    'MiningAmount': 200.0,
    'CriticalSuccessChance': 0.01,  # 1%
    'ResidueVolumeMultiplier': 0,
    'ResidueProbability': 0,
    'TechLevel': 1,
    'CriticalSuccessBonusYield': 2.0,  # 200%
    'MetaLevel': 6
}

# Base stats for Modulated Strip Miner II
MODULATED_BASE_STATS = {
    'ActivationCost': 30.0,
    'StructureHitpoints': 40,
    'Volume': 5,
    'Capacity': 10,
    'OptimalRange': 15.00,
    'ActivationTime': 45.0,
    'MiningAmount': 120.0,
    'CriticalSuccessChance': 0.01,  # 1%
    'ResidueVolumeMultiplier': 1.0,  # 1x
    'ResidueProbability': 0.34,  # 34%
    'TechLevel': 2,
    'CriticalSuccessBonusYield': 2.0,  # 200%
    'MetaLevel': 5
}

# Mutation ranges (same for both)
MUTATIONS = {
    'ActivationCost': {'Min': -0.40, 'Max': 0.40},
    'ActivationTime': {'Min': -0.10, 'Max': 0.10},
    'CPUUsage': {'Min': -0.20, 'Max': 0.50},
    'CriticalSuccessBonusYield': {'Min': -0.20, 'Max': 0.15},
    'CriticalSuccessChance': {'Min': -0.35, 'Max': 0.30},
    'MiningAmount': {'Min': -0.15, 'Max': 0.30},
    'OptimalRange': {'Min': -0.25, 'Max': 0.30},
    'PowergridUsage': {'Min': -0.20, 'Max': 0.50},
    'ResidueProbability': {'Min': -0.30, 'Max': 0.30},
    'ResidueVolumeMultiplier': {'Min': -0.20, 'Max': 0.15}
}

# Mining Skills (Level 5 = max, each gives 5% per level)
MINING_SKILLS = {
    'Mining': 5,        # 25% bonus (1.25x)
    'Astrogeology': 5,  # 25% bonus (1.25x)
    'Exhumer': 5        # 25% bonus (1.25x)
}

# Ship Role Bonus (for Exhumers)
SHIP_ROLE_BONUS = 1.75  # 75% total = 1.75x

# Mining Laser Upgrade II modules (3 modules × 5% each = 15% = 1.15x)
MODULE_BONUS = 1.15

# Rorqual Industrial Core Bonuses
RORQUAL_BOOSTS = {
    'MiningForemanBurstYield': 1.15,  # 15% yield bonus
    'IndustrialCoreYield': 1.50,       # 50% yield bonus
    'IndustrialCoreCycleTime': 0.75    # 25% cycle time reduction
}

# Calibration multiplier to match Pyfa values
CALIBRATION_MULTIPLIER = 1.35

# Tier ranges
ORE_TIER_RANGES = {
    'S': {'Min': 6.27, 'Max': 6.61},
    'A': {'Min': 5.92, 'Max': 6.27},
    'B': {'Min': 5.57, 'Max': 5.92},
    'C': {'Min': 5.23, 'Max': 5.57},
    'D': {'Min': 4.88, 'Max': 5.23},
    'E': {'Min': 4.44, 'Max': 4.88},
    'F': {'Min': 0, 'Max': 4.44}
}

MODULATED_TIER_RANGES = {
    'S': {'Min': 3.76188, 'Max': 3.97},
    'A': {'Min': 3.55376, 'Max': 3.76188},
    'B': {'Min': 3.34564, 'Max': 3.55376},
    'C': {'Min': 3.13752, 'Max': 3.34564},
    'D': {'Min': 2.92940, 'Max': 3.13752},
    'E': {'Min': 2.67, 'Max': 2.92940},
    'F': {'Min': 0, 'Max': 2.67}
}

# ============================================================================
# CALCULATION FUNCTIONS
# ============================================================================

def get_skill_bonus():
    """Calculate skill bonuses multiplier"""
    mining_bonus = 1 + (MINING_SKILLS['Mining'] * 0.05)  # 1.25x
    astro_bonus = 1 + (MINING_SKILLS['Astrogeology'] * 0.05)  # 1.25x
    exhumer_bonus = 1 + (MINING_SKILLS['Exhumer'] * 0.05)  # 1.25x
    return mining_bonus * astro_bonus * exhumer_bonus

def calculate_base_m3_per_sec(mining_amount, activation_time):
    """Calculate base m3/sec"""
    if activation_time <= 0:
        raise ValueError("ActivationTime cannot be zero or negative")
    return mining_amount / activation_time

def calculate_effective_m3_per_sec(mining_amount, activation_time, crit_chance, crit_bonus, 
                                   residue_probability=0, residue_multiplier=0):
    """Calculate effective m3/sec with crits and residue"""
    if activation_time <= 0:
        raise ValueError("ActivationTime cannot be zero or negative")
    
    base_m3 = mining_amount
    crit_gain = base_m3 * crit_bonus * crit_chance
    residue_loss = base_m3 * residue_probability * residue_multiplier
    expected_m3_per_cycle = base_m3 + crit_gain - residue_loss
    return expected_m3_per_cycle / activation_time

def calculate_base_plus_crits_m3_per_sec(mining_amount, activation_time, crit_chance, crit_bonus):
    """Calculate base + crits m3/sec (no residue)"""
    base_m3 = mining_amount
    crit_gain = base_m3 * crit_bonus * crit_chance
    expected_m3_per_cycle = base_m3 + crit_gain
    return expected_m3_per_cycle / activation_time

def calculate_real_world_base_m3_per_sec(base_mining_amount, base_activation_time):
    """Calculate real-world base m3/sec with all bonuses"""
    if base_activation_time <= 0:
        raise ValueError("ActivationTime cannot be zero or negative")
    
    skill_multiplier = get_skill_bonus()
    bonused_mining_amount = base_mining_amount * skill_multiplier
    bonused_mining_amount = bonused_mining_amount * SHIP_ROLE_BONUS
    bonused_mining_amount = bonused_mining_amount * MODULE_BONUS
    
    boosted_mining_amount = bonused_mining_amount * RORQUAL_BOOSTS['MiningForemanBurstYield'] * RORQUAL_BOOSTS['IndustrialCoreYield']
    boosted_activation_time = base_activation_time * RORQUAL_BOOSTS['IndustrialCoreCycleTime']
    
    if boosted_activation_time <= 0:
        raise ValueError("Boosted ActivationTime cannot be zero or negative")
    
    boosted_mining_amount = boosted_mining_amount * CALIBRATION_MULTIPLIER
    return boosted_mining_amount / boosted_activation_time

def calculate_real_world_effective_m3_per_sec(base_mining_amount, base_activation_time, crit_chance, 
                                            crit_bonus, residue_probability=0, residue_multiplier=0):
    """Calculate real-world effective m3/sec with all bonuses"""
    if base_activation_time <= 0:
        raise ValueError("ActivationTime cannot be zero or negative")
    
    skill_multiplier = get_skill_bonus()
    bonused_mining_amount = base_mining_amount * skill_multiplier
    bonused_mining_amount = bonused_mining_amount * SHIP_ROLE_BONUS
    bonused_mining_amount = bonused_mining_amount * MODULE_BONUS
    
    boosted_mining_amount = bonused_mining_amount * RORQUAL_BOOSTS['MiningForemanBurstYield'] * RORQUAL_BOOSTS['IndustrialCoreYield']
    boosted_activation_time = base_activation_time * RORQUAL_BOOSTS['IndustrialCoreCycleTime']
    
    if boosted_activation_time <= 0:
        raise ValueError("Boosted ActivationTime cannot be zero or negative")
    
    boosted_mining_amount = boosted_mining_amount * CALIBRATION_MULTIPLIER
    
    base_m3 = boosted_mining_amount
    crit_gain = base_m3 * crit_bonus * crit_chance
    residue_loss = base_m3 * residue_probability * residue_multiplier
    expected_m3_per_cycle = base_m3 + crit_gain - residue_loss
    return expected_m3_per_cycle / boosted_activation_time

# ============================================================================
# PARSING FUNCTIONS
# ============================================================================

def parse_item_stats(clipboard_text):
    """Parse item stats from clipboard text"""
    stats = {}
    lines = clipboard_text.split('\n')
    
    for line in lines:
        line = line.strip()
        if not line:
            continue
        
        # Match pattern: "Label" followed by whitespace then number
        match = re.match(r'^(.+?)\s+(\d+\.?\d*)', line)
        if match:
            label = match.group(1).strip()
            num_value = float(match.group(2))
            label_lower = label.lower()
            
            if re.match(r'^activation\s+cost', label_lower):
                stats['ActivationCost'] = num_value
            elif re.match(r'activation\s+time', label_lower) or (re.match(r'duration', label_lower) and 'residue' not in label_lower):
                stats['ActivationTime'] = num_value
            elif re.match(r'^mining\s+amount', label_lower):
                stats['MiningAmount'] = num_value
            elif re.match(r'critical\s+success\s+chance', label_lower):
                stats['CriticalSuccessChance'] = num_value / 100.0
            elif re.match(r'critical\s+success\s+bonus\s+yield', label_lower):
                stats['CriticalSuccessBonusYield'] = num_value / 100.0
            elif re.match(r'^optimal\s+range', label_lower):
                stats['OptimalRange'] = num_value
            elif re.match(r'residue\s+probability', label_lower):
                stats['ResidueProbability'] = num_value / 100.0
            elif re.match(r'residue\s+volume\s+multiplier', label_lower):
                stats['ResidueVolumeMultiplier'] = num_value
    
    return stats

# ============================================================================
# ANALYSIS FUNCTIONS
# ============================================================================

def analyze_roll(stats, base_stats, miner_type):
    """Analyze a roll and return results"""
    # Fill in defaults from base stats
    mutated_stats = base_stats.copy()
    mutated_stats.update(stats)
    
    # Calculate m3/sec
    m3_per_sec = calculate_base_m3_per_sec(mutated_stats['MiningAmount'], mutated_stats['ActivationTime'])
    
    # Calculate effective m3/sec
    if miner_type == 'ORE':
        effective_m3_per_sec = calculate_effective_m3_per_sec(
            mutated_stats['MiningAmount'],
            mutated_stats['ActivationTime'],
            mutated_stats['CriticalSuccessChance'],
            mutated_stats['CriticalSuccessBonusYield']
        )
        base_plus_crits_m3_per_sec = None
    else:  # Modulated
        base_plus_crits_m3_per_sec = calculate_base_plus_crits_m3_per_sec(
            mutated_stats['MiningAmount'],
            mutated_stats['ActivationTime'],
            mutated_stats['CriticalSuccessChance'],
            mutated_stats['CriticalSuccessBonusYield']
        )
        effective_m3_per_sec = calculate_effective_m3_per_sec(
            mutated_stats['MiningAmount'],
            mutated_stats['ActivationTime'],
            mutated_stats['CriticalSuccessChance'],
            mutated_stats['CriticalSuccessBonusYield'],
            mutated_stats['ResidueProbability'],
            mutated_stats['ResidueVolumeMultiplier']
        )
    
    # Calculate real-world values
    real_world_m3_per_sec = calculate_real_world_base_m3_per_sec(
        mutated_stats['MiningAmount'],
        mutated_stats['ActivationTime']
    )
    
    if miner_type == 'ORE':
        real_world_effective_m3_per_sec = calculate_real_world_effective_m3_per_sec(
            mutated_stats['MiningAmount'],
            mutated_stats['ActivationTime'],
            mutated_stats['CriticalSuccessChance'],
            mutated_stats['CriticalSuccessBonusYield']
        )
        real_world_base_plus_crits_m3_per_sec = None
    else:  # Modulated
        real_world_base_plus_crits_m3_per_sec = calculate_real_world_effective_m3_per_sec(
            mutated_stats['MiningAmount'],
            mutated_stats['ActivationTime'],
            mutated_stats['CriticalSuccessChance'],
            mutated_stats['CriticalSuccessBonusYield'],
            0, 0
        )
        real_world_effective_m3_per_sec = calculate_real_world_effective_m3_per_sec(
            mutated_stats['MiningAmount'],
            mutated_stats['ActivationTime'],
            mutated_stats['CriticalSuccessChance'],
            mutated_stats['CriticalSuccessBonusYield'],
            mutated_stats['ResidueProbability'],
            mutated_stats['ResidueVolumeMultiplier']
        )
    
    # Determine tier
    tier_ranges = ORE_TIER_RANGES if miner_type == 'ORE' else MODULATED_TIER_RANGES
    tier = 'F'
    
    if m3_per_sec >= tier_ranges['S']['Min']:
        tier = 'S'
    elif m3_per_sec >= tier_ranges['A']['Min'] and m3_per_sec < tier_ranges['A']['Max']:
        tier = 'A'
    elif m3_per_sec >= tier_ranges['B']['Min'] and m3_per_sec < tier_ranges['B']['Max']:
        tier = 'B'
    elif m3_per_sec >= tier_ranges['C']['Min'] and m3_per_sec < tier_ranges['C']['Max']:
        tier = 'C'
    elif m3_per_sec >= tier_ranges['D']['Min'] and m3_per_sec < tier_ranges['D']['Max']:
        tier = 'D'
    elif m3_per_sec >= tier_ranges['E']['Min'] and m3_per_sec < tier_ranges['E']['Max']:
        tier = 'E'
    elif m3_per_sec < tier_ranges['F']['Max']:
        tier = 'F'
    
    return {
        'Stats': mutated_stats,
        'M3PerSec': m3_per_sec,
        'BasePlusCritsM3PerSec': base_plus_crits_m3_per_sec,
        'EffectiveM3PerSec': effective_m3_per_sec,
        'RealWorldM3PerSec': real_world_m3_per_sec,
        'RealWorldBasePlusCritsM3PerSec': real_world_base_plus_crits_m3_per_sec,
        'RealWorldEffectiveM3PerSec': real_world_effective_m3_per_sec,
        'Tier': tier
    }

# ============================================================================
# GUI APPLICATION
# ============================================================================

def detect_system_theme():
    """Detect system theme (light/dark)"""
    import platform
    
    if platform.system() == "Windows":
        try:
            import winreg
            key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Software\Microsoft\Windows\CurrentVersion\Themes\Personalize")
            value, _ = winreg.QueryValueEx(key, "AppsUseLightTheme")
            winreg.CloseKey(key)
            return "light" if value == 1 else "dark"
        except:
            pass
    elif platform.system() == "Darwin":  # macOS
        try:
            import subprocess
            result = subprocess.run(['defaults', 'read', '-g', 'AppleInterfaceStyle'], 
                                  capture_output=True, text=True)
            if 'Dark' in result.stdout:
                return "dark"
        except:
            pass
    # Linux - try to detect from gsettings
    elif platform.system() == "Linux":
        try:
            import subprocess
            result = subprocess.run(['gsettings', 'get', 'org.gnome.desktop.interface', 'gtk-theme'],
                                  capture_output=True, text=True)
            if 'dark' in result.stdout.lower():
                return "dark"
        except:
            pass
    
    # Default to light if detection fails
    return "light"

class MinerAnalyzerApp:
    def __init__(self, root):
        self.root = root
        self.root.title("EVE Online Strip Miner Roll Analyzer")
        self.root.geometry("900x700")
        
        # Detect system theme
        self.theme = detect_system_theme()
        self.setup_theme()
        
        # Miner type
        self.miner_type = tk.StringVar(value="ORE")
        self.last_clipboard_hash = None
        self.monitoring = False
        
        self.setup_ui()
        self.start_monitoring()
    
    def setup_theme(self):
        """Setup theme based on system preference"""
        if self.theme == "dark":
            self.bg_color = "#1e1e1e"
            self.fg_color = "#ffffff"
            self.frame_bg = "#2d2d2d"
            self.entry_bg = "#3d3d3d"
            self.text_bg = "#2d2d2d"
            self.tier_colors = {
                'S': '#00ff00',
                'A': '#00ffff',
                'B': '#0080ff',
                'C': '#ffff00',
                'D': '#ff00ff',
                'E': '#ffa500',
                'F': '#ff0000'
            }
        else:  # light
            self.bg_color = "#ffffff"
            self.fg_color = "#000000"
            self.frame_bg = "#f0f0f0"
            self.entry_bg = "#ffffff"
            self.text_bg = "#ffffff"
            self.tier_colors = {
                'S': '#00aa00',
                'A': '#00aaaa',
                'B': '#0066cc',
                'C': '#ccaa00',
                'D': '#aa00aa',
                'E': '#cc6600',
                'F': '#cc0000'
            }
        
        self.root.configure(bg=self.bg_color)
    
    def setup_ui(self):
        """Setup the user interface"""
        # Header frame
        header_frame = tk.Frame(self.root, bg=self.bg_color)
        header_frame.pack(fill=tk.X, padx=10, pady=10)
        
        title_label = tk.Label(header_frame, text="EVE Online Strip Miner Roll Analyzer", 
                              font=("Arial", 16, "bold"), bg=self.bg_color, fg=self.fg_color)
        title_label.pack()
        
        # Miner type selection
        type_frame = tk.Frame(header_frame, bg=self.bg_color)
        type_frame.pack(pady=10)
        
        tk.Label(type_frame, text="Miner Type:", bg=self.bg_color, fg=self.fg_color).pack(side=tk.LEFT, padx=5)
        ore_radio = tk.Radiobutton(type_frame, text="ORE", variable=self.miner_type, value="ORE",
                                   command=self.on_miner_type_change, bg=self.bg_color, fg=self.fg_color,
                                   selectcolor=self.frame_bg)
        ore_radio.pack(side=tk.LEFT, padx=5)
        
        mod_radio = tk.Radiobutton(type_frame, text="Modulated", variable=self.miner_type, value="Modulated",
                                   command=self.on_miner_type_change, bg=self.bg_color, fg=self.fg_color,
                                   selectcolor=self.frame_bg)
        mod_radio.pack(side=tk.LEFT, padx=5)
        
        # Status label
        self.status_label = tk.Label(header_frame, text="Monitoring clipboard...", 
                                     bg=self.bg_color, fg=self.fg_color)
        self.status_label.pack(pady=5)
        
        # Results frame with scrollbar
        results_frame = tk.Frame(self.root, bg=self.bg_color)
        results_frame.pack(fill=tk.BOTH, expand=True, padx=10, pady=10)
        
        # Text widget with scrollbar
        scrollbar = tk.Scrollbar(results_frame)
        scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        
        self.results_text = tk.Text(results_frame, wrap=tk.WORD, yscrollcommand=scrollbar.set,
                                   bg=self.text_bg, fg=self.fg_color, font=("Consolas", 10),
                                   state=tk.DISABLED)
        self.results_text.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar.config(command=self.results_text.yview)
        
        # Configure text tags for colors
        for tier, color in self.tier_colors.items():
            self.results_text.tag_config(f"tier_{tier}", foreground=color, font=("Consolas", 10, "bold"))
        self.results_text.tag_config("good", foreground="#00aa00" if self.theme == "light" else "#00ff00")
        self.results_text.tag_config("bad", foreground="#cc0000" if self.theme == "light" else "#ff0000")
        self.results_text.tag_config("header", font=("Consolas", 11, "bold"))
    
    def on_miner_type_change(self):
        """Handle miner type change"""
        self.update_status("Miner type changed. Waiting for clipboard update...")
        self.results_text.config(state=tk.NORMAL)
        self.results_text.delete(1.0, tk.END)
        self.results_text.insert(tk.END, f"Miner type set to: {self.miner_type.get()}\n")
        self.results_text.insert(tk.END, "Copy item stats from EVE Online to analyze.\n")
        self.results_text.config(state=tk.DISABLED)
    
    def update_status(self, message):
        """Update status label"""
        timestamp = datetime.now().strftime("%H:%M:%S")
        self.status_label.config(text=f"{message} - {timestamp}")
    
    def format_percentage(self, value):
        """Format percentage with sign"""
        if value > 0.1:
            return f"+{value:.1f}%"
        elif value < -0.1:
            return f"{value:.1f}%"
        else:
            return "+0.0%"
    
    def get_color_tag(self, value):
        """Get color tag based on value"""
        if value > 0.1:
            return "good"
        elif value < -0.1:
            return "bad"
        return ""
    
    def display_analysis(self, analysis, base_stats, miner_type):
        """Display analysis results"""
        self.results_text.config(state=tk.NORMAL)
        self.results_text.delete(1.0, tk.END)
        
        stats = analysis['Stats']
        
        # Header
        self.results_text.insert(tk.END, "=" * 76 + "\n", "header")
        self.results_text.insert(tk.END, f"EVE Online {miner_type} Strip Miner Roll Analyzer\n", "header")
        self.results_text.insert(tk.END, "=" * 76 + "\n\n", "header")
        
        # Roll Analysis
        self.results_text.insert(tk.END, "Roll Analysis:\n", "header")
        self.results_text.insert(tk.END, f"{'Metric':<20} {'Base':<20} {'Rolled':<20} {'% Change':<20}\n")
        self.results_text.insert(tk.END, "-" * 76 + "\n")
        
        # Mining Amount
        mining_mut = ((stats['MiningAmount'] / base_stats['MiningAmount']) - 1) * 100
        tag = self.get_color_tag(mining_mut)
        self.results_text.insert(tk.END, f"{'Mining Amount':<20} ")
        self.results_text.insert(tk.END, f"{base_stats['MiningAmount']:.1f} m3{'':<12} ")
        self.results_text.insert(tk.END, f"{stats['MiningAmount']:.1f} m3{'':<12} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(mining_mut)}\n", tag)
        
        # Activation Time
        time_mut = ((stats['ActivationTime'] / base_stats['ActivationTime']) - 1) * 100
        tag = self.get_color_tag(-time_mut)  # Negative is good for time
        self.results_text.insert(tk.END, f"{'Activation Time':<20} ")
        self.results_text.insert(tk.END, f"{base_stats['ActivationTime']:.1f} s{'':<13} ")
        self.results_text.insert(tk.END, f"{stats['ActivationTime']:.1f} s{'':<13} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(time_mut)}\n", tag)
        
        # Crit Chance
        crit_chance_mut = ((stats['CriticalSuccessChance'] / base_stats['CriticalSuccessChance']) - 1) * 100 if base_stats['CriticalSuccessChance'] > 0 else 0
        tag = self.get_color_tag(crit_chance_mut)
        self.results_text.insert(tk.END, f"{'Crit Chance':<20} ")
        self.results_text.insert(tk.END, f"{base_stats['CriticalSuccessChance']:.2%}{'':<15} ")
        self.results_text.insert(tk.END, f"{stats['CriticalSuccessChance']:.2%}{'':<15} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(crit_chance_mut)}\n", tag)
        
        # Crit Bonus
        crit_bonus_mut = ((stats['CriticalSuccessBonusYield'] / base_stats['CriticalSuccessBonusYield']) - 1) * 100
        tag = self.get_color_tag(crit_bonus_mut)
        self.results_text.insert(tk.END, f"{'Crit Bonus':<20} ")
        self.results_text.insert(tk.END, f"{base_stats['CriticalSuccessBonusYield']:.0%}{'':<16} ")
        self.results_text.insert(tk.END, f"{stats['CriticalSuccessBonusYield']:.0%}{'':<16} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(crit_bonus_mut)}\n", tag)
        
        # Residue (Modulated only)
        if miner_type == "Modulated":
            residue_prob_mut = ((stats['ResidueProbability'] / base_stats['ResidueProbability']) - 1) * 100
            tag = self.get_color_tag(-residue_prob_mut)  # Negative is good
            self.results_text.insert(tk.END, f"{'Residue Prob':<20} ")
            self.results_text.insert(tk.END, f"{base_stats['ResidueProbability']:.2%}{'':<15} ")
            self.results_text.insert(tk.END, f"{stats['ResidueProbability']:.2%}{'':<15} ", tag)
            self.results_text.insert(tk.END, f"{self.format_percentage(residue_prob_mut)}\n", tag)
            
            residue_mult_mut = ((stats['ResidueVolumeMultiplier'] / base_stats['ResidueVolumeMultiplier']) - 1) * 100
            tag = self.get_color_tag(-residue_mult_mut)  # Negative is good
            self.results_text.insert(tk.END, f"{'Residue Mult':<20} ")
            self.results_text.insert(tk.END, f"{base_stats['ResidueVolumeMultiplier']:.3f} x{'':<14} ")
            self.results_text.insert(tk.END, f"{stats['ResidueVolumeMultiplier']:.3f} x{'':<14} ", tag)
            self.results_text.insert(tk.END, f"{self.format_percentage(residue_mult_mut)}\n", tag)
        
        # Optimal Range
        optimal_range_mut = ((stats['OptimalRange'] / base_stats['OptimalRange']) - 1) * 100 if base_stats['OptimalRange'] > 0 else 0
        tag = self.get_color_tag(optimal_range_mut)
        self.results_text.insert(tk.END, f"{'Optimal Range':<20} ")
        self.results_text.insert(tk.END, f"{base_stats['OptimalRange']:.2f} km{'':<14} ")
        self.results_text.insert(tk.END, f"{stats['OptimalRange']:.2f} km{'':<14} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(optimal_range_mut)}\n", tag)
        
        self.results_text.insert(tk.END, "\n")
        
        # Performance Metrics
        self.results_text.insert(tk.END, "Performance Metrics:\n", "header")
        self.results_text.insert(tk.END, f"{'Metric':<20} {'Base':<20} {'Rolled':<20} {'% Change':<20}\n")
        self.results_text.insert(tk.END, "-" * 76 + "\n")
        
        # Calculate base values for comparison
        base_m3_per_sec = calculate_base_m3_per_sec(base_stats['MiningAmount'], base_stats['ActivationTime'])
        base_effective_m3_per_sec = calculate_effective_m3_per_sec(
            base_stats['MiningAmount'],
            base_stats['ActivationTime'],
            base_stats['CriticalSuccessChance'],
            base_stats['CriticalSuccessBonusYield'],
            base_stats.get('ResidueProbability', 0),
            base_stats.get('ResidueVolumeMultiplier', 0)
        )
        base_real_world_m3_per_sec = calculate_real_world_base_m3_per_sec(
            base_stats['MiningAmount'],
            base_stats['ActivationTime']
        )
        base_real_world_effective_m3_per_sec = calculate_real_world_effective_m3_per_sec(
            base_stats['MiningAmount'],
            base_stats['ActivationTime'],
            base_stats['CriticalSuccessChance'],
            base_stats['CriticalSuccessBonusYield'],
            base_stats.get('ResidueProbability', 0),
            base_stats.get('ResidueVolumeMultiplier', 0)
        )
        
        # Base M3/sec
        base_m3_pct = ((analysis['M3PerSec'] / base_m3_per_sec) - 1) * 100 if base_m3_per_sec > 0 else 0
        tag = self.get_color_tag(base_m3_pct)
        self.results_text.insert(tk.END, f"{'Base M3/sec':<20} ")
        self.results_text.insert(tk.END, f"{base_m3_per_sec:.2f} ({base_real_world_m3_per_sec:.1f}){'':<6} ")
        self.results_text.insert(tk.END, f"{analysis['M3PerSec']:.2f} ({analysis['RealWorldM3PerSec']:.1f}){'':<6} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(base_m3_pct)}\n", tag)
        
        # Base + Crits (Modulated only)
        if miner_type == "Modulated" and analysis['BasePlusCritsM3PerSec'] is not None:
            base_base_plus_crits = calculate_base_plus_crits_m3_per_sec(
                base_stats['MiningAmount'],
                base_stats['ActivationTime'],
                base_stats['CriticalSuccessChance'],
                base_stats['CriticalSuccessBonusYield']
            )
            base_real_world_base_plus_crits = calculate_real_world_effective_m3_per_sec(
                base_stats['MiningAmount'],
                base_stats['ActivationTime'],
                base_stats['CriticalSuccessChance'],
                base_stats['CriticalSuccessBonusYield'],
                0, 0  # No residue for base+crits
            )
            base_plus_crits_pct = ((analysis['BasePlusCritsM3PerSec'] / base_base_plus_crits) - 1) * 100 if base_base_plus_crits > 0 else 0
            tag = self.get_color_tag(base_plus_crits_pct)
            self.results_text.insert(tk.END, f"{'Base + Crits M3/s':<20} ")
            self.results_text.insert(tk.END, f"{base_base_plus_crits:.2f} ({base_real_world_base_plus_crits:.1f}){'':<6} ")
            self.results_text.insert(tk.END, f"{analysis['BasePlusCritsM3PerSec']:.2f} ({analysis['RealWorldBasePlusCritsM3PerSec']:.1f}){'':<6} ", tag)
            self.results_text.insert(tk.END, f"{self.format_percentage(base_plus_crits_pct)}\n", tag)
        
        # Effective M3/sec
        eff_m3_pct = ((analysis['EffectiveM3PerSec'] / base_effective_m3_per_sec) - 1) * 100 if base_effective_m3_per_sec > 0 else 0
        tag = self.get_color_tag(eff_m3_pct)
        self.results_text.insert(tk.END, f"{'Effective M3/sec':<20} ")
        self.results_text.insert(tk.END, f"{base_effective_m3_per_sec:.2f} ({base_real_world_effective_m3_per_sec:.1f}){'':<6} ")
        self.results_text.insert(tk.END, f"{analysis['EffectiveM3PerSec']:.2f} ({analysis['RealWorldEffectiveM3PerSec']:.1f}){'':<6} ", tag)
        self.results_text.insert(tk.END, f"{self.format_percentage(eff_m3_pct)}\n", tag)
        
        self.results_text.insert(tk.END, "\n")
        
        # Tier
        tier = analysis['Tier']
        tier_tag = f"tier_{tier}"
        tier_ranges = ORE_TIER_RANGES if miner_type == "ORE" else MODULATED_TIER_RANGES
        tier_range = tier_ranges[tier]
        
        if tier == 'S':
            tier_range_str = f"{tier_range['Min']:.2f}-{tier_range['Max']:.2f}+ m³/s"
        elif tier == 'F':
            tier_range_str = f"<{tier_range['Max']:.2f} m³/s"
        else:
            tier_range_str = f"{tier_range['Min']:.2f}-{tier_range['Max']:.5f} m³/s"
        
        self.results_text.insert(tk.END, f"Tier: ", "header")
        self.results_text.insert(tk.END, f"{tier}\n", tier_tag)
        self.results_text.insert(tk.END, f"({tier_range_str})\n", tier_tag)
        
        self.results_text.insert(tk.END, "\n" + "=" * 76 + "\n")
        
        self.results_text.config(state=tk.DISABLED)
        
        # Copy to clipboard
        tier_display = f"+{tier}" if tier == 'S' else tier
        miner_label = "[ORE]" if miner_type == "ORE" else "[Modulated]"
        clipboard_text = f"{tier_display}: ({self.format_percentage(base_m3_pct)}) {miner_label}"
        try:
            pyperclip.copy(clipboard_text)
        except:
            pass
    
    def check_clipboard(self):
        """Check clipboard for changes and analyze"""
        try:
            clipboard_text = pyperclip.paste()
            if clipboard_text:
                current_hash = hash(clipboard_text)
                if current_hash != self.last_clipboard_hash:
                    self.last_clipboard_hash = current_hash
                    
                    # Parse stats
                    parsed_stats = parse_item_stats(clipboard_text)
                    
                    if parsed_stats:
                        miner_type = self.miner_type.get()
                        base_stats = ORE_BASE_STATS if miner_type == "ORE" else MODULATED_BASE_STATS
                        
                        # Analyze
                        analysis = analyze_roll(parsed_stats, base_stats, miner_type)
                        
                        # Display
                        self.display_analysis(analysis, base_stats, miner_type)
                        self.update_status("Analysis complete")
        except Exception as e:
            self.update_status(f"Error: {str(e)}")
    
    def start_monitoring(self):
        """Start clipboard monitoring in background thread"""
        self.monitoring = True
        
        def monitor():
            while self.monitoring:
                self.check_clipboard()
                time.sleep(0.3)  # Check every 300ms
        
        thread = threading.Thread(target=monitor, daemon=True)
        thread.start()
    
    def stop_monitoring(self):
        """Stop clipboard monitoring"""
        self.monitoring = False

def main():
    root = tk.Tk()
    app = MinerAnalyzerApp(root)
    
    def on_closing():
        app.stop_monitoring()
        root.destroy()
    
    root.protocol("WM_DELETE_WINDOW", on_closing)
    root.mainloop()

if __name__ == "__main__":
    main()

