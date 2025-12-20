# EVE Online Strip Miner Roll Analyzer
# Reads item stats from clipboard and calculates m3/sec with mutation ranges
#
# Usage:
#   1. Copy the item stats from EVE Online (Ctrl+C on the item info window)
#   2. Run this script: .\roll_analyzer.ps1
#   3. The script will analyze the current roll and show tier ranges
#
# The script calculates:
#   - Base m3/sec (mining amount / activation time)
#   - Effective m3/sec (including critical success bonuses)
#   - Tier assignment (S, A, B, C, D, E, F)
#   - Roll ranges for each tier showing mutation percentages

# Base stats for ORE Strip Miner
$baseStats = @{
    ActivationCost = 23.0
    StructureHitpoints = 40
    Volume = 5
    OptimalRange = 18.75
    ActivationTime = 45.0
    MiningAmount = 200.0
    CriticalSuccessChance = 0.01  # 1%
    ResidueVolumeMultiplier = 0
    ResidueProbability = 0
    TechLevel = 1
    CriticalSuccessBonusYield = 2.0  # 200%
    MetaLevel = 6
}

# Mutation ranges from Unstable Strip Miner Mutaplasmid
$mutations = @{
    ActivationCost = @{ Min = -0.40; Max = 0.40 }      # +40% to -40%
    ActivationTime = @{ Min = -0.10; Max = 0.10 }      # +10% to -10%
    CPUUsage = @{ Min = -0.20; Max = 0.50 }            # +50% to -20%
    CriticalSuccessBonusYield = @{ Min = -0.20; Max = 0.15 }  # -20% to +15%
    CriticalSuccessChance = @{ Min = -0.35; Max = 0.30 }       # -35% to +30%
    MiningAmount = @{ Min = -0.15; Max = 0.30 }        # -15% to +30%
    OptimalRange = @{ Min = -0.25; Max = 0.30 }        # -25% to +30%
    PowergridUsage = @{ Min = -0.20; Max = 0.50 }      # +50% to -20%
}

# ============================================================================
# CONFIGURATION: Max Skills and Rorqual Boosts
# ============================================================================

# Mining Skills (Level 5 = max, each gives 5% per level)
$miningSkills = @{
    Mining = 5              # 25% bonus (1.25x)
    Astrogeology = 5        # 25% bonus (1.25x)
    Exhumer = 5             # 25% bonus (1.25x) - for Exhumers (Hulk, Mackinaw, Skiff)
}

# Ship Role Bonus (for Exhumers - Hulk, Mackinaw, Skiff)
# Each Exhumer level gives 15% bonus (5 levels = 75% total = 1.75x)
$shipRoleBonus = 1.75

# Mining Laser Upgrade II modules (3 modules × 5% each = 15% = 1.15x)
$moduleBonus = 1.15

# Rorqual Industrial Core Bonuses (Tech II, max skills)
$rorqualBoosts = @{
    MiningForemanBurstYield = 1.15      # 15% yield bonus from Mining Foreman Burst
    IndustrialCoreYield = 1.50          # 50% yield bonus from Industrial Core
    IndustrialCoreCycleTime = 0.75       # 25% cycle time reduction (0.75x = 25% faster)
}

# Calibration multiplier to match Pyfa values
# Base ORE: 200/45 = 4.44 m³/s, Pyfa shows 56.3 m³/s = 12.68x multiplier
# Rolled item: 216.44/47.1 = 4.59 m³/s, Pyfa shows 60.3 m³/s
# Adjusted to match actual Pyfa values
$calibrationMultiplier = 1.35


# Function to calculate base m3/sec
function Calculate-BaseM3PerSec {
    param($miningAmount, $activationTime)
    if ($activationTime -eq 0 -or $activationTime -lt 0) {
        throw "ActivationTime cannot be zero or negative"
    }
    return $miningAmount / $activationTime
}

# Function to calculate effective m3/sec with crits
function Calculate-EffectiveM3PerSec {
    param($miningAmount, $activationTime, $critChance, $critBonus)
    
    if ($activationTime -eq 0 -or $activationTime -lt 0) {
        throw "ActivationTime cannot be zero or negative"
    }
    
    # Base m3 per cycle
    $baseM3 = $miningAmount
    
    # Expected m3 per cycle including crits
    # Formula: base * (1 - critChance) + base * (1 + critBonus) * critChance
    # Simplified: base * (1 + critBonus * critChance)
    $expectedM3PerCycle = $baseM3 * (1 + $critBonus * $critChance)
    
    # m3/sec
    return $expectedM3PerCycle / $activationTime
}

# Calculate skill bonuses multiplier
function Get-SkillBonus {
    # Each skill gives 5% per level, all multiplicative
    $miningBonus = 1 + ($miningSkills.Mining * 0.05)           # 1.25x
    $astroBonus = 1 + ($miningSkills.Astrogeology * 0.05)       # 1.25x
    $exhumerBonus = 1 + ($miningSkills.Exhumer * 0.05)         # 1.25x
    
    # All bonuses are multiplicative
    return $miningBonus * $astroBonus * $exhumerBonus
}

# Calculate real-world base m3/sec with all bonuses applied
function Calculate-RealWorldBaseM3PerSec {
    param($baseMiningAmount, $baseActivationTime)
    
    # Apply skill bonuses (Mining + Astrogeology + Exhumer)
    $skillMultiplier = Get-SkillBonus
    $bonusedMiningAmount = $baseMiningAmount * $skillMultiplier
    
    # Apply ship role bonus (Exhumer)
    $bonusedMiningAmount = $bonusedMiningAmount * $shipRoleBonus
    
    # Apply Mining Laser Upgrade II modules (3 modules × 5% each)
    $bonusedMiningAmount = $bonusedMiningAmount * $moduleBonus
    
    # Apply Rorqual boosts
    $boostedMiningAmount = $bonusedMiningAmount * $rorqualBoosts.MiningForemanBurstYield * $rorqualBoosts.IndustrialCoreYield
    $boostedActivationTime = $baseActivationTime * $rorqualBoosts.IndustrialCoreCycleTime
    
    # Apply calibration multiplier to match Pyfa values
    $boostedMiningAmount = $boostedMiningAmount * $calibrationMultiplier
    
    # Calculate m3/sec
    return $boostedMiningAmount / $boostedActivationTime
}

# Calculate real-world effective m3/sec with crits and all bonuses
function Calculate-RealWorldEffectiveM3PerSec {
    param($baseMiningAmount, $baseActivationTime, $critChance, $critBonus)
    
    # Apply skill bonuses (Mining + Astrogeology + Exhumer)
    $skillMultiplier = Get-SkillBonus
    $bonusedMiningAmount = $baseMiningAmount * $skillMultiplier
    
    # Apply ship role bonus (Exhumer)
    $bonusedMiningAmount = $bonusedMiningAmount * $shipRoleBonus
    
    # Apply Mining Laser Upgrade II modules (3 modules × 5% each)
    $bonusedMiningAmount = $bonusedMiningAmount * $moduleBonus
    
    # Apply Rorqual boosts
    $boostedMiningAmount = $bonusedMiningAmount * $rorqualBoosts.MiningForemanBurstYield * $rorqualBoosts.IndustrialCoreYield
    $boostedActivationTime = $baseActivationTime * $rorqualBoosts.IndustrialCoreCycleTime
    
    # Apply calibration multiplier to match Pyfa values
    $boostedMiningAmount = $boostedMiningAmount * $calibrationMultiplier
    
    # Base m3 per cycle (after all bonuses)
    $baseM3 = $boostedMiningAmount
    
    # Expected m3 per cycle including crits (gain)
    $critGain = $baseM3 * $critBonus * $critChance
    
    # Expected m3 per cycle = base + crit gain
    $expectedM3PerCycle = $baseM3 + $critGain
    
    # m3/sec
    return $expectedM3PerCycle / $boostedActivationTime
}

# Function to parse clipboard content
function Parse-ItemStats {
    param($clipboardText)
    
    $stats = @{}
    $lines = $clipboardText -split "`r?`n"
    
    foreach ($line in $lines) {
        $line = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        
        # Debug: uncomment to see what lines are being processed
        # Write-Host "Processing line: $line" -ForegroundColor Gray
        
        # Try to match various stat formats (case insensitive)
        # Handle tab-separated or space-separated values
        # Format: "Label\tValue" or "Label    Value" (multiple spaces/tabs)
        
        # Match pattern: "Label" followed by whitespace then number (with optional units)
        # This regex matches: label, then whitespace, then number (which may have units after)
        # The number can be a decimal like 48.2 or 171.98
        # Pattern: non-greedy match for label, then whitespace, then number (integer or decimal)
        if ($line -match '^(.+?)\s+(\d+\.?\d*)') {
            $label = $matches[1].Trim()
            $numValue = [double]$matches[2]
            
            # Match labels (case insensitive, flexible whitespace)
            $labelLower = $label.ToLower()
            
            # Match "Activation Cost"
            if ($labelLower -match '^activation\s+cost') {
                $stats['ActivationCost'] = $numValue
            }
            # Match "Activation time / duration" or just "duration"
            elseif ($labelLower -match 'activation\s+time' -or ($labelLower -match 'duration' -and -not $labelLower -match 'residue')) {
                $stats['ActivationTime'] = $numValue
            }
            # Match "Mining amount"
            elseif ($labelLower -match '^mining\s+amount') {
                $stats['MiningAmount'] = $numValue
            }
            # Match "Critical Success Chance"
            elseif ($labelLower -match 'critical\s+success\s+chance') {
                $stats['CriticalSuccessChance'] = $numValue / 100.0
            }
            # Match "Critical Success Bonus Yield"
            elseif ($labelLower -match 'critical\s+success\s+bonus\s+yield') {
                $stats['CriticalSuccessBonusYield'] = $numValue / 100.0
            }
            # Match "Optimal Range"
            elseif ($labelLower -match '^optimal\s+range') {
                $stats['OptimalRange'] = $numValue
            }
        }
    }
    
    return $stats
}

# Function to generate all possible roll combinations and calculate m3/sec
function Generate-RollRanges {
    param($baseStats, $mutations)
    
    # Calculate base m3/sec
    $baseM3PerSec = Calculate-BaseM3PerSec $baseStats.MiningAmount $baseStats.ActivationTime
    $baseEffectiveM3PerSec = Calculate-EffectiveM3PerSec `
        $baseStats.MiningAmount `
        $baseStats.ActivationTime `
        $baseStats.CriticalSuccessChance `
        $baseStats.CriticalSuccessBonusYield
    
    # Calculate best case (all positive mutations)
    $bestMiningAmount = $baseStats.MiningAmount * (1 + $mutations.MiningAmount.Max)
    $bestActivationTime = $baseStats.ActivationTime * (1 + $mutations.ActivationTime.Min)  # Lower time is better
    $bestCritChance = [Math]::Max(0, $baseStats.CriticalSuccessChance * (1 + $mutations.CriticalSuccessChance.Max))
    $bestCritBonus = $baseStats.CriticalSuccessBonusYield * (1 + $mutations.CriticalSuccessBonusYield.Max)
    
    $bestM3PerSec = Calculate-BaseM3PerSec $bestMiningAmount $bestActivationTime
    $bestEffectiveM3PerSec = Calculate-EffectiveM3PerSec $bestMiningAmount $bestActivationTime $bestCritChance $bestCritBonus
    
    # Calculate worst case (all negative mutations)
    $worstMiningAmount = $baseStats.MiningAmount * (1 + $mutations.MiningAmount.Min)
    $worstActivationTime = $baseStats.ActivationTime * (1 + $mutations.ActivationTime.Max)  # Higher time is worse
    $worstCritChance = [Math]::Max(0, $baseStats.CriticalSuccessChance * (1 + $mutations.CriticalSuccessChance.Min))
    $worstCritBonus = $baseStats.CriticalSuccessBonusYield * (1 + $mutations.CriticalSuccessBonusYield.Min)
    
    $worstM3PerSec = Calculate-BaseM3PerSec $worstMiningAmount $worstActivationTime
    $worstEffectiveM3PerSec = Calculate-EffectiveM3PerSec $worstMiningAmount $worstActivationTime $worstCritChance $worstCritBonus
    
    # Calculate real-world boosted values for base stats
    $baseRealWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $baseStats.MiningAmount $baseStats.ActivationTime
    $baseRealWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec `
        $baseStats.MiningAmount `
        $baseStats.ActivationTime `
        $baseStats.CriticalSuccessChance `
        $baseStats.CriticalSuccessBonusYield
    
    # Calculate real-world boosted values for best case
    $bestRealWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $bestMiningAmount $bestActivationTime
    $bestRealWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec $bestMiningAmount $bestActivationTime $bestCritChance $bestCritBonus
    
    # Calculate real-world boosted values for worst case
    $worstRealWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $worstMiningAmount $worstActivationTime
    $worstRealWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec $worstMiningAmount $worstActivationTime $worstCritChance $worstCritBonus
    
    # Define fixed tier ranges based on BASE m3/s values (not effective)
    # S: 6.27 – 6.61+ m³/s (BASE)
    # A: 5.92 – 6.27 m³/s (BASE)
    # B: 5.57 – 5.92 m³/s (BASE)
    # C: 5.23 – 5.57 m³/s (BASE)
    # D: 4.88 – 5.23 m³/s (BASE)
    # E: 4.44 – 4.88 m³/s (BASE)
    # F: <4.44 m³/s (BASE)
    
    $tierRanges = @{
        'S' = @{ Min = 6.27; Max = $bestM3PerSec }    # 6.27 – 6.61+ m³/s (BASE)
        'A' = @{ Min = 5.92; Max = 6.27 }             # 5.92 – 6.27 m³/s (BASE)
        'B' = @{ Min = 5.57; Max = 5.92 }             # 5.57 – 5.92 m³/s (BASE)
        'C' = @{ Min = 5.23; Max = 5.57 }             # 5.23 – 5.57 m³/s (BASE)
        'D' = @{ Min = 4.88; Max = 5.23 }             # 4.88 – 5.23 m³/s (BASE)
        'E' = @{ Min = 4.44; Max = 4.88 }             # 4.44 – 4.88 m³/s (BASE)
        'F' = @{ Min = $worstM3PerSec; Max = 4.44 }   # <4.44 m³/s (BASE)
    }
    
    # Tier roll ranges - use full mutation ranges for all tiers
    $tierRollRanges = @{}
    foreach ($tier in @('S', 'A', 'B', 'C', 'D', 'E', 'F')) {
        $tierMin = $tierRanges[$tier].Min
        $tierMax = $tierRanges[$tier].Max
        
        $tierRollRanges[$tier] = @{
            MiningAmount = @{ Min = $mutations.MiningAmount.Min; Max = $mutations.MiningAmount.Max }
            ActivationTime = @{ Min = $mutations.ActivationTime.Min; Max = $mutations.ActivationTime.Max }
            CritChance = @{ Min = $mutations.CriticalSuccessChance.Min; Max = $mutations.CriticalSuccessChance.Max }
            CritBonus = @{ Min = $mutations.CriticalSuccessBonusYield.Min; Max = $mutations.CriticalSuccessBonusYield.Max }
            EffectiveM3PerSec = @{ Min = $tierMin; Max = $tierMax }
        }
    }
    
    return @{
        BaseM3PerSec = $baseM3PerSec
        BaseEffectiveM3PerSec = $baseEffectiveM3PerSec
        BaseRealWorldM3PerSec = $baseRealWorldM3PerSec
        BaseRealWorldEffectiveM3PerSec = $baseRealWorldEffectiveM3PerSec
        BestM3PerSec = $bestM3PerSec
        BestEffectiveM3PerSec = $bestEffectiveM3PerSec
        BestRealWorldM3PerSec = $bestRealWorldM3PerSec
        BestRealWorldEffectiveM3PerSec = $bestRealWorldEffectiveM3PerSec
        WorstM3PerSec = $worstM3PerSec
        WorstEffectiveM3PerSec = $worstEffectiveM3PerSec
        WorstRealWorldM3PerSec = $worstRealWorldM3PerSec
        WorstRealWorldEffectiveM3PerSec = $worstRealWorldEffectiveM3PerSec
        TierRanges = $tierRanges
        TierRollRanges = $tierRollRanges
    }
}

# Function to display roll analysis
function Show-RollAnalysis {
    param($currentStats, $baseStats, $mutations, $rollRanges, $showTierRanges = $false)
    
    # Analyze current roll (pass rollRanges to avoid recalculating)
    $analysis = Analyze-Roll $currentStats $baseStats $mutations $rollRanges
    
    Clear-Host
    Write-Host ""
    Write-Host ("  " + ("=" * 76))
    Write-Host "  EVE Online Strip Miner Roll Analyzer - Real-Time Mode"
    Write-Host ("  " + ("=" * 76))
    Write-Host ""
    
    # Calculate mutation percentages
    $miningMut = (($analysis.Stats.MiningAmount / $baseStats.MiningAmount) - 1) * 100
    $timeMut = (($analysis.Stats.ActivationTime / $baseStats.ActivationTime) - 1) * 100
    $critChanceMut = if ($baseStats.CriticalSuccessChance -gt 0) { (($analysis.Stats.CriticalSuccessChance / $baseStats.CriticalSuccessChance) - 1) * 100 } else { 0 }
    $critBonusMut = (($analysis.Stats.CriticalSuccessBonusYield / $baseStats.CriticalSuccessBonusYield) - 1) * 100
    $optimalRangeMut = if ($baseStats.OptimalRange -gt 0) { (($analysis.Stats.OptimalRange / $baseStats.OptimalRange) - 1) * 100 } else { 0 }
    
    Write-Host "  Roll Analysis:"
    Write-Host ("  {0,-20} {1,-20} {2,-20} {3,-20}" -f "Metric", "Base", "Rolled", "% Change")
    Write-Host ("  " + ("-" * 76))
    
    # Mining Amount: + is good, - is bad
    $miningColor = if ($miningMut -gt 0.1) { 'Green' } elseif ($miningMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  {0,-20} " -f "Mining Amount") -NoNewline
    Write-Host ("{0,-20} " -f "$($baseStats.MiningAmount.ToString('N1')) m3") -NoNewline
    Write-Host ("{0,-20} " -f "$($analysis.Stats.MiningAmount.ToString('N1')) m3") -ForegroundColor $miningColor -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $miningMut) -ForegroundColor $miningColor
    
    # Activation Time: - is good (faster), + is bad (slower)
    $timeColor = if ($timeMut -lt -0.1) { 'Green' } elseif ($timeMut -gt 0.1) { 'Red' } else { 'White' }
    Write-Host ("  {0,-20} " -f "Activation Time") -NoNewline
    Write-Host ("{0,-20} " -f "$($baseStats.ActivationTime.ToString('N1')) s") -NoNewline
    Write-Host ("{0,-20} " -f "$($analysis.Stats.ActivationTime.ToString('N1')) s") -ForegroundColor $timeColor -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $timeMut) -ForegroundColor $timeColor
    
    # Crit Chance: + is good, - is bad
    $critChanceColor = if ($critChanceMut -gt 0.1) { 'Green' } elseif ($critChanceMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  {0,-20} " -f "Crit Chance") -NoNewline
    Write-Host ("{0,-20} " -f "$($baseStats.CriticalSuccessChance.ToString('P2'))") -NoNewline
    Write-Host ("{0,-20} " -f "$($analysis.Stats.CriticalSuccessChance.ToString('P2'))") -ForegroundColor $critChanceColor -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $critChanceMut) -ForegroundColor $critChanceColor
    
    # Crit Bonus: + is good, - is bad
    $critBonusColor = if ($critBonusMut -gt 0.1) { 'Green' } elseif ($critBonusMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  {0,-20} " -f "Crit Bonus") -NoNewline
    Write-Host ("{0,-20} " -f "$($baseStats.CriticalSuccessBonusYield.ToString('P0'))") -NoNewline
    Write-Host ("{0,-20} " -f "$($analysis.Stats.CriticalSuccessBonusYield.ToString('P0'))") -ForegroundColor $critBonusColor -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $critBonusMut) -ForegroundColor $critBonusColor
    
    # Optimal Range: + is good, - is bad
    $optimalRangeColor = if ($optimalRangeMut -gt 0.1) { 'Green' } elseif ($optimalRangeMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  {0,-20} " -f "Optimal Range") -NoNewline
    Write-Host ("{0,-20} " -f "$($baseStats.OptimalRange.ToString('N2')) km") -NoNewline
    Write-Host ("{0,-20} " -f "$($analysis.Stats.OptimalRange.ToString('N2')) km") -ForegroundColor $optimalRangeColor -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $optimalRangeMut) -ForegroundColor $optimalRangeColor
    Write-Host ""
    
    Write-Host "  Performance Metrics:"
    Write-Host ("  {0,-20} {1,-20} {2,-20} {3,-20}" -f "Metric", "Base", "Rolled", "% Change")
    Write-Host ("  " + ("-" * 76))
    
    # Base M3/sec - with boosted values in parentheses
    $baseM3Pct = if ($rollRanges.BaseM3PerSec -gt 0) { (($analysis.M3PerSec / $rollRanges.BaseM3PerSec) - 1) * 100 } else { 0 }
    $baseM3Color = if ($baseM3Pct -gt 1) { 'Green' } elseif ($baseM3Pct -lt -1) { 'Red' } else { 'White' }
    $baseValueStr = "{0:N2} ({1:N1})" -f $rollRanges.BaseM3PerSec, $rollRanges.BaseRealWorldM3PerSec
    $rolledValueStr = "{0:N2} ({1:N1})" -f $analysis.M3PerSec, $analysis.RealWorldM3PerSec
    Write-Host ("  {0,-20} " -f "Base M3/sec") -NoNewline
    Write-Host ("{0,-20} " -f $baseValueStr) -NoNewline
    Write-Host ("{0,-20} " -f $rolledValueStr) -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $baseM3Pct) -ForegroundColor $baseM3Color
    
    # Effective M3/sec - with boosted values in parentheses
    $effM3Pct = if ($rollRanges.BaseEffectiveM3PerSec -gt 0) { (($analysis.EffectiveM3PerSec / $rollRanges.BaseEffectiveM3PerSec) - 1) * 100 } else { 0 }
    $effM3Color = if ($effM3Pct -gt 1) { 'Green' } elseif ($effM3Pct -lt -1) { 'Red' } else { 'White' }
    $effBaseValueStr = "{0:N2} ({1:N1})" -f $rollRanges.BaseEffectiveM3PerSec, $rollRanges.BaseRealWorldEffectiveM3PerSec
    $effRolledValueStr = "{0:N2} ({1:N1})" -f $analysis.EffectiveM3PerSec, $analysis.RealWorldEffectiveM3PerSec
    Write-Host ("  {0,-20} " -f "Effective M3/sec") -NoNewline
    Write-Host ("{0,-20} " -f $effBaseValueStr) -NoNewline
    Write-Host ("{0,-20} " -f $effRolledValueStr) -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $effM3Pct) -ForegroundColor $effM3Color
    Write-Host ""
    
    # Color code the tier
    $tierColor = switch ($analysis.Tier) {
        'S' { 'Green' }
        'A' { 'Cyan' }
        'B' { 'Blue' }
        'C' { 'Yellow' }
        'D' { 'Magenta' }
        'E' { 'DarkYellow' }
        'F' { 'Red' }
        default { 'White' }
    }
    
    # Get tier range for display
    $tierRange = $rollRanges.TierRanges[$analysis.Tier]
    if ($analysis.Tier -eq 'S' -and $tierRange.Max -gt 6.61) {
        $tierRangeStr = "{0:N2}-{1:N2}+ m³/s" -f $tierRange.Min, 6.61
    } elseif ($analysis.Tier -eq 'F') {
        $tierRangeStr = "<{0:N2} m³/s" -f $tierRange.Max
    } else {
        $tierRangeStr = "{0:N2}-{1:N2} m³/s" -f $tierRange.Min, $tierRange.Max
    }
    
    Write-Host ("  Tier: ") -NoNewline
    Write-Host ("{0}" -f $analysis.Tier) -ForegroundColor $tierColor
    Write-Host ("  ({0})" -f $tierRangeStr) -ForegroundColor $tierColor
    Write-Host ""
    
    # Set clipboard with tier and base percentage for easy container naming
    # Format: "Tier: (+##.#%) [ORE]" e.g., "+S (+23.3%) [ORE]" or "C (+23.3%) [ORE]"
    $tierDisplay = if ($analysis.Tier -eq 'S') { "+S" } else { $analysis.Tier }
    $clipboardText = "{0}: ({1:+0.0;-0.0;+0.0}%) [ORE]" -f $tierDisplay, $baseM3Pct
    try {
        Set-Clipboard -Value $clipboardText
    } catch {
        # Silently fail if clipboard can't be set (some terminals may not support this)
    }
    
    Write-Host ("  " + ("=" * 76))
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "  Monitoring clipboard... Last update: $timestamp (Press Ctrl+C to exit)"
    Write-Host ("  " + ("=" * 76))
}

# Function to analyze a specific roll
function Analyze-Roll {
    param($stats, $baseStats, $mutations, $rollRanges)
    
    # Calculate mutated stats
    $mutatedStats = @{}
    foreach ($key in $stats.Keys) {
        if ($baseStats.ContainsKey($key)) {
            $mutatedStats[$key] = $stats[$key]
        }
    }
    
    # Fill in defaults from base stats
    foreach ($key in $baseStats.Keys) {
        if (-not $mutatedStats.ContainsKey($key)) {
            $mutatedStats[$key] = $baseStats[$key]
        }
    }
    
    # Calculate m3/sec
    $m3PerSec = Calculate-BaseM3PerSec $mutatedStats.MiningAmount $mutatedStats.ActivationTime
    $effectiveM3PerSec = Calculate-EffectiveM3PerSec `
        $mutatedStats.MiningAmount `
        $mutatedStats.ActivationTime `
        $mutatedStats.CriticalSuccessChance `
        $mutatedStats.CriticalSuccessBonusYield
    
    # Calculate real-world boosted m3/sec (with all bonuses)
    $realWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $mutatedStats.MiningAmount $mutatedStats.ActivationTime
    $realWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec `
        $mutatedStats.MiningAmount `
        $mutatedStats.ActivationTime `
        $mutatedStats.CriticalSuccessChance `
        $mutatedStats.CriticalSuccessBonusYield
    
    # Determine tier (use passed rollRanges instead of recalculating)
    $tier = "F"
    
    # Check tiers from highest (S) to lowest (F) using fixed m3/s ranges
    # Order: S -> A -> B -> C -> D -> E -> F
    # Note: Tier assignment is based on BASE m³/s, not effective
    
    # S tier: >= 6.27 m³/s (BASE)
    if ($m3PerSec -ge $rollRanges.TierRanges['S'].Min) {
        $tier = 'S'
    }
    # A tier: >= 5.92 and < 6.27 m³/s (BASE)
    elseif ($m3PerSec -ge $rollRanges.TierRanges['A'].Min -and $m3PerSec -lt $rollRanges.TierRanges['A'].Max) {
        $tier = 'A'
    }
    # B tier: >= 5.57 and < 5.92 m³/s (BASE)
    elseif ($m3PerSec -ge $rollRanges.TierRanges['B'].Min -and $m3PerSec -lt $rollRanges.TierRanges['B'].Max) {
        $tier = 'B'
    }
    # C tier: >= 5.23 and < 5.57 m³/s (BASE)
    elseif ($m3PerSec -ge $rollRanges.TierRanges['C'].Min -and $m3PerSec -lt $rollRanges.TierRanges['C'].Max) {
        $tier = 'C'
    }
    # D tier: >= 4.88 and < 5.23 m³/s (BASE)
    elseif ($m3PerSec -ge $rollRanges.TierRanges['D'].Min -and $m3PerSec -lt $rollRanges.TierRanges['D'].Max) {
        $tier = 'D'
    }
    # E tier: >= 4.44 and < 4.88 m³/s (BASE)
    elseif ($m3PerSec -ge $rollRanges.TierRanges['E'].Min -and $m3PerSec -lt $rollRanges.TierRanges['E'].Max) {
        $tier = 'E'
    }
    # F tier: < 4.44 m³/s (BASE) (worst, catch-all)
    elseif ($m3PerSec -lt $rollRanges.TierRanges['F'].Max) {
        $tier = 'F'
    }
    # Fallback to F (shouldn't happen, but safety catch-all)
    else {
        $tier = 'F'
    }
    
    # Calculate crit impact
    $baseEffective = $rollRanges.BaseEffectiveM3PerSec
    $critImpact = $effectiveM3PerSec - $baseEffective
    $critSign = if ($critImpact -gt 0) { "+" } else { "" }
    
    return @{
        Stats = $mutatedStats
        M3PerSec = $m3PerSec
        EffectiveM3PerSec = $effectiveM3PerSec
        RealWorldM3PerSec = $realWorldM3PerSec
        RealWorldEffectiveM3PerSec = $realWorldEffectiveM3PerSec
        Tier = $tier
        CritImpact = $critImpact
        CritSign = $critSign
    }
}

# Main execution
# Set window size (width, height in characters)
try {
    $buffer = $Host.UI.RawUI.BufferSize
    $window = $Host.UI.RawUI.WindowSize
    
    # Set window size: width 82 (80 content + 2 indent), height 40 lines
    $window.Width = 82
    $window.Height = 40
    
    # Ensure window size doesn't exceed buffer size
    if ($window.Width -gt $buffer.Width) {
        $buffer.Width = $window.Width
        $Host.UI.RawUI.BufferSize = $buffer
    }
    if ($window.Height -gt $buffer.Height) {
        $buffer.Height = $window.Height
        $Host.UI.RawUI.BufferSize = $buffer
    }
    
    $Host.UI.RawUI.WindowSize = $window
} catch {
    # If setting window size fails, continue anyway (may not work in all terminals)
    # This is normal in some terminals like VS Code integrated terminal
}

Write-Host ""
Write-Host ("=" * 80)
Write-Host "EVE Online Strip Miner Roll Analyzer - Real-Time Mode"
Write-Host ("=" * 80)
Write-Host ""
Write-Host "This tool will monitor your clipboard and update automatically when you copy new item stats."
Write-Host "Press Ctrl+C to exit."
Write-Host ""

# Generate roll ranges (now instant with fixed ranges)
$rollRanges = Generate-RollRanges $baseStats $mutations

Write-Host "Ready to analyze items!" -ForegroundColor Green
Write-Host ""

# Track last clipboard content to detect changes
$lastClipboardText = $null
$lastClipboardHash = $null
$firstRun = $true
$lastProcessedTime = Get-Date

# Main monitoring loop
while ($true) {
    try {
        # Try to get clipboard as text (try different methods)
        $clipboardText = $null
        try {
            $clipboardText = Get-Clipboard -Format Text -ErrorAction Stop
        } catch {
            # Fallback: try without format specification
            try {
                $clipboardText = Get-Clipboard -ErrorAction Stop
            } catch {
                throw
            }
        }
        
        # Handle null clipboard
        if ($null -eq $clipboardText) {
            Start-Sleep -Milliseconds 300
            continue
        }
        
        # Convert to string if it's not already
        if ($clipboardText -isnot [string]) {
            $clipboardText = $clipboardText.ToString()
        }
        
        # Handle empty
        if ([string]::IsNullOrWhiteSpace($clipboardText)) {
            Start-Sleep -Milliseconds 300
            continue
        }
        
        # Use hash to detect actual content changes (more efficient than string comparison)
        $currentHash = $clipboardText.GetHashCode()
        
        # Only process if clipboard content actually changed
        if ($currentHash -ne $lastClipboardHash -and $clipboardText.Length -gt 0) {
            $lastClipboardHash = $currentHash
            $lastClipboardText = $clipboardText
            $currentTime = Get-Date
            
            # Parse item stats
            $parsedStats = Parse-ItemStats $clipboardText
            
            # Use parsed stats or fall back to base stats
            $currentStats = $baseStats.Clone()
            $parsedCount = 0
            foreach ($key in $parsedStats.Keys) {
                $currentStats[$key] = $parsedStats[$key]
                $parsedCount++
            }
            
            if ($parsedCount -gt 0) {
                # Show analysis (don't show tier ranges by default)
                Show-RollAnalysis $currentStats $baseStats $mutations $rollRanges $false
                $firstRun = $false
                $lastProcessedTime = $currentTime
            } elseif ($firstRun) {
                $lastClipboardText = $clipboardText
                # On first run, show message if nothing parsed
                Clear-Host
                Write-Host ""
                Write-Host ("=" * 80)
                Write-Host "EVE Online Strip Miner Roll Analyzer - Real-Time Mode"
                Write-Host ("=" * 80)
                Write-Host ""
                Write-Host "Waiting for item stats in clipboard..."
                Write-Host ""
                Write-Host "Instructions:"
                Write-Host "  1. Copy item stats from EVE Online (Ctrl+C on item info window)"
                Write-Host "  2. The analysis will appear automatically"
                Write-Host "  3. Copy new items to see updated analysis"
                Write-Host ""
                Write-Host "Debug: Clipboard detected but couldn't parse stats."
                Write-Host "       Clipboard length: $($clipboardText.Length) characters"
                Write-Host "       First 200 chars: $($clipboardText.Substring(0, [Math]::Min(200, $clipboardText.Length)))"
                Write-Host ""
                Write-Host "Press Ctrl+C to exit."
                Write-Host ("=" * 80)
                $firstRun = $false
            } elseif (($currentTime - $lastProcessedTime).TotalSeconds -gt 5) {
                # Show debug info if we haven't processed anything in a while
                Write-Host ""
                Write-Host "Debug: Clipboard changed but couldn't parse stats." -ForegroundColor Yellow
                Write-Host "       Clipboard length: $($clipboardText.Length) characters" -ForegroundColor Yellow
                if ($clipboardText.Length -gt 0) {
                    $preview = $clipboardText.Substring(0, [Math]::Min(200, $clipboardText.Length))
                    Write-Host "       Preview: $preview" -ForegroundColor Yellow
                }
                Write-Host ""
                $lastProcessedTime = $currentTime
            }
        }
        
        # Small delay to avoid excessive CPU usage
        Start-Sleep -Milliseconds 300
    } catch {
        # Handle clipboard errors gracefully
        $errorMsg = $_.Exception.Message
        if ($firstRun) {
            Clear-Host
            Write-Host ""
            Write-Host ("=" * 80)
            Write-Host "EVE Online Strip Miner Roll Analyzer - Real-Time Mode"
            Write-Host ("=" * 80)
            Write-Host ""
            Write-Host "Error reading clipboard: $errorMsg" -ForegroundColor Red
            Write-Host ""
            Write-Host "Troubleshooting:"
            Write-Host "  1. Make sure you have copied item stats to the clipboard"
            Write-Host "  2. Try copying the item stats again (Ctrl+C on item info window)"
            Write-Host "  3. The script will retry automatically"
            Write-Host ""
            Write-Host "Press Ctrl+C to exit."
            Write-Host ("=" * 80)
            $firstRun = $false
        }
        Start-Sleep -Milliseconds 1000
    }
}

