# EVE Online Modulated Strip Miner II Roll Analyzer - Real-World Performance
# Accounts for max skills and Rorqual Industrial Core boosts
# Based on roll-analyzer-modulated.ps1
#
# Usage:
#   1. Copy the item stats from EVE Online (Ctrl+C on the item info window)
#   2. Run this script: .\roll-analyzer-modulated-realworld.ps1
#   3. The script will analyze the current roll with real-world bonuses applied
#
# The script calculates:
#   - Real-World Base m3/sec (with max skills + Rorqual boosts)
#   - Real-World Effective m3/sec (including crits, residue, and all bonuses)
#   - Tier assignment (S, A, B, C, D, E, F) based on real-world values

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

# Mining Crystal Bonus (Simple Asteroid Mining Crystal Type B II)
# This is a significant multiplier - reverse engineered from in-game values
$miningCrystalBonus = 3.06  # Type B II crystal bonus (adjust if needed)

# Rorqual Industrial Core Bonuses (Tech II, max skills)
$rorqualBoosts = @{
    MiningForemanBurstYield = 1.15      # 15% yield bonus from Mining Foreman Burst
    IndustrialCoreYield = 1.50          # 50% yield bonus from Industrial Core
    IndustrialCoreCycleTime = 0.75       # 25% cycle time reduction (0.75x = 25% faster)
}

# ============================================================================
# BASE STATS (from game data - Base Value column)
# ============================================================================

# Base stats for Modulated Strip Miner II (unmutated)
$baseStats = @{
    ActivationCost = 30.0
    StructureHitpoints = 40
    Volume = 5
    Capacity = 10
    OptimalRange = 15.00
    ActivationTime = 45.0
    MiningAmount = 120.0
    CriticalSuccessChance = 0.01  # 1%
    ResidueVolumeMultiplier = 1.0  # 1x
    ResidueProbability = 0.34  # 34%
    TechLevel = 2
    CriticalSuccessBonusYield = 2.0  # 200%
    MetaLevel = 5
}

# Mutation ranges from Unstable Modulated Strip Miner Mutaplasmid
$mutations = @{
    ActivationCost = @{ Min = -0.40; Max = 0.40 }      # +40% to -40%
    ActivationTime = @{ Min = -0.10; Max = 0.10 }      # +10% to -10%
    CPUUsage = @{ Min = -0.20; Max = 0.50 }            # +50% to -20%
    CriticalSuccessBonusYield = @{ Min = -0.20; Max = 0.15 }  # -20% to +15%
    CriticalSuccessChance = @{ Min = -0.35; Max = 0.30 }       # -35% to +30%
    MiningAmount = @{ Min = -0.15; Max = 0.30 }        # -15% to +30%
    OptimalRange = @{ Min = -0.25; Max = 0.30 }        # -25% to +30%
    PowergridUsage = @{ Min = -0.20; Max = 0.50 }      # +50% to -20%
    ResidueProbability = @{ Min = -0.30; Max = 0.30 }  # +30% to -30%
    ResidueVolumeMultiplier = @{ Min = -0.20; Max = 0.15 }  # +15% to -20%
}

# ============================================================================
# CALCULATION FUNCTIONS WITH REAL-WORLD BONUSES
# ============================================================================

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
    
    # Apply Mining Crystal bonus (Simple Asteroid Mining Crystal Type B II)
    $bonusedMiningAmount = $bonusedMiningAmount * $miningCrystalBonus
    
    # Apply Rorqual boosts
    $boostedMiningAmount = $bonusedMiningAmount * $rorqualBoosts.MiningForemanBurstYield * $rorqualBoosts.IndustrialCoreYield
    $boostedActivationTime = $baseActivationTime * $rorqualBoosts.IndustrialCoreCycleTime
    
    # Calculate m3/sec
    return $boostedMiningAmount / $boostedActivationTime
}

# Calculate real-world base + crits m3/sec (with crits but NO residue)
function Calculate-RealWorldBasePlusCritsM3PerSec {
    param(
        $baseMiningAmount, 
        $baseActivationTime, 
        $critChance, 
        $critBonus
    )
    
    # Apply skill bonuses
    $skillMultiplier = Get-SkillBonus
    $bonusedMiningAmount = $baseMiningAmount * $skillMultiplier
    
    # Apply ship role bonus
    $bonusedMiningAmount = $bonusedMiningAmount * $shipRoleBonus
    
    # Apply Mining Laser Upgrade II modules
    $bonusedMiningAmount = $bonusedMiningAmount * $moduleBonus
    
    # Apply Mining Crystal bonus (Simple Asteroid Mining Crystal Type B II)
    $bonusedMiningAmount = $bonusedMiningAmount * $miningCrystalBonus
    
    # Apply Rorqual boosts
    $boostedMiningAmount = $bonusedMiningAmount * $rorqualBoosts.MiningForemanBurstYield * $rorqualBoosts.IndustrialCoreYield
    $boostedActivationTime = $baseActivationTime * $rorqualBoosts.IndustrialCoreCycleTime
    
    # Base m3 per cycle (after all bonuses)
    $baseM3 = $boostedMiningAmount
    
    # Expected m3 per cycle including crits (gain) - NO residue loss
    $critGain = $baseM3 * $critBonus * $critChance
    
    # Expected m3 per cycle = base + crit gain (no residue subtraction)
    $expectedM3PerCycle = $baseM3 + $critGain
    
    # m3/sec
    return $expectedM3PerCycle / $boostedActivationTime
}

# Calculate real-world effective m3/sec with crits, residue, and all bonuses
function Calculate-RealWorldEffectiveM3PerSec {
    param(
        $baseMiningAmount, 
        $baseActivationTime, 
        $critChance, 
        $critBonus, 
        $residueProbability, 
        $residueMultiplier
    )
    
    # Apply skill bonuses
    $skillMultiplier = Get-SkillBonus
    $bonusedMiningAmount = $baseMiningAmount * $skillMultiplier
    
    # Apply ship role bonus
    $bonusedMiningAmount = $bonusedMiningAmount * $shipRoleBonus
    
    # Apply Mining Laser Upgrade II modules
    $bonusedMiningAmount = $bonusedMiningAmount * $moduleBonus
    
    # Apply Mining Crystal bonus (Simple Asteroid Mining Crystal Type B II)
    $bonusedMiningAmount = $bonusedMiningAmount * $miningCrystalBonus
    
    # Apply Rorqual boosts
    $boostedMiningAmount = $bonusedMiningAmount * $rorqualBoosts.MiningForemanBurstYield * $rorqualBoosts.IndustrialCoreYield
    $boostedActivationTime = $baseActivationTime * $rorqualBoosts.IndustrialCoreCycleTime
    
    # Base m3 per cycle (after all bonuses)
    $baseM3 = $boostedMiningAmount
    
    # Expected m3 per cycle including crits (gain)
    $critGain = $baseM3 * $critBonus * $critChance
    
    # Expected m3 lost to residue (loss)
    $residueLoss = $baseM3 * $residueProbability * $residueMultiplier
    
    # Net expected m3 per cycle = base + crit gain - residue loss
    $expectedM3PerCycle = $baseM3 + $critGain - $residueLoss
    
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
        
        # Handle tab-separated or space-separated values
        $parts = $line -split "`t", 2
        if ($parts.Length -eq 1) {
            $parts = $line -split '\s{2,}', 2
        }
        if ($parts.Length -eq 1) {
            if ($line -match '^(.+?)\s+(\d+\.?\d*)') {
                $parts = @($matches[1].Trim(), $matches[2])
            }
        }
        
        if ($parts.Length -ge 2) {
            $label = $parts[0].Trim()
            $valueStr = $parts[1].Trim()
            
            # Extract number from value (handle cases like "40.7s", "146.66 m3", "36.141998859 %")
            if ($valueStr -match '(\d+\.?\d*(?:\.\d+)?)') {
                $numValue = [double]$matches[1]
                
                $labelLower = $label.ToLower()
                
                if ($labelLower -match '^activation\s+cost') {
                    $stats['ActivationCost'] = $numValue
                }
                elseif ($labelLower -match 'activation\s+time' -or ($labelLower -match 'duration' -and -not $labelLower -match 'residue')) {
                    $stats['ActivationTime'] = $numValue
                }
                elseif ($labelLower -match 'mining\s+amount') {
                    $stats['MiningAmount'] = $numValue
                }
                elseif ($labelLower -match 'critical\s+success\s+chance') {
                    $stats['CriticalSuccessChance'] = $numValue / 100.0
                }
                elseif ($labelLower -match 'critical\s+success\s+bonus\s+yield') {
                    $stats['CriticalSuccessBonusYield'] = $numValue / 100.0
                }
                elseif ($labelLower -match 'optimal\s+range') {
                    $stats['OptimalRange'] = $numValue
                }
                elseif ($labelLower -match 'residue\s+probability') {
                    $stats['ResidueProbability'] = $numValue / 100.0
                }
                elseif ($labelLower -match 'residue\s+volume\s+multiplier') {
                    $stats['ResidueVolumeMultiplier'] = $numValue
                }
            }
        }
    }
    
    return $stats
}

# Function to generate roll ranges with real-world bonuses
function Generate-RollRanges {
    param($baseStats, $mutations)
    
    # Calculate base real-world m3/sec (unmutated, with all bonuses)
    $baseRealWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $baseStats.MiningAmount $baseStats.ActivationTime
    $baseRealWorldBasePlusCritsM3PerSec = Calculate-RealWorldBasePlusCritsM3PerSec `
        $baseStats.MiningAmount `
        $baseStats.ActivationTime `
        $baseStats.CriticalSuccessChance `
        $baseStats.CriticalSuccessBonusYield
    $baseRealWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec `
        $baseStats.MiningAmount `
        $baseStats.ActivationTime `
        $baseStats.CriticalSuccessChance `
        $baseStats.CriticalSuccessBonusYield `
        $baseStats.ResidueProbability `
        $baseStats.ResidueVolumeMultiplier
    
    # Calculate best case (all positive mutations)
    $bestMiningAmount = $baseStats.MiningAmount * (1 + $mutations.MiningAmount.Max)
    $bestActivationTime = $baseStats.ActivationTime * (1 + $mutations.ActivationTime.Min)
    $bestCritChance = [Math]::Max(0, $baseStats.CriticalSuccessChance * (1 + $mutations.CriticalSuccessChance.Max))
    $bestCritBonus = $baseStats.CriticalSuccessBonusYield * (1 + $mutations.CriticalSuccessBonusYield.Max)
    $bestResidueProb = [Math]::Max(0, $baseStats.ResidueProbability * (1 + $mutations.ResidueProbability.Min))
    $bestResidueMult = [Math]::Max(0, $baseStats.ResidueVolumeMultiplier * (1 + $mutations.ResidueVolumeMultiplier.Min))
    
    $bestRealWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $bestMiningAmount $bestActivationTime
    $bestRealWorldBasePlusCritsM3PerSec = Calculate-RealWorldBasePlusCritsM3PerSec $bestMiningAmount $bestActivationTime $bestCritChance $bestCritBonus
    $bestRealWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec $bestMiningAmount $bestActivationTime $bestCritChance $bestCritBonus $bestResidueProb $bestResidueMult
    
    # Calculate worst case (all negative mutations)
    $worstMiningAmount = $baseStats.MiningAmount * (1 + $mutations.MiningAmount.Min)
    $worstActivationTime = $baseStats.ActivationTime * (1 + $mutations.ActivationTime.Max)
    $worstCritChance = [Math]::Max(0, $baseStats.CriticalSuccessChance * (1 + $mutations.CriticalSuccessChance.Min))
    $worstCritBonus = $baseStats.CriticalSuccessBonusYield * (1 + $mutations.CriticalSuccessBonusYield.Min)
    $worstResidueProb = [Math]::Min(1, $baseStats.ResidueProbability * (1 + $mutations.ResidueProbability.Max))
    $worstResidueMult = $baseStats.ResidueVolumeMultiplier * (1 + $mutations.ResidueVolumeMultiplier.Max)
    
    $worstRealWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $worstMiningAmount $worstActivationTime
    $worstRealWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec $worstMiningAmount $worstActivationTime $worstCritChance $worstCritBonus $worstResidueProb $worstResidueMult
    
    # Calculate boost multiplier for scaling tier ranges
    $baseUnboostedM3PerSec = $baseStats.MiningAmount / $baseStats.ActivationTime
    $boostMultiplier = $baseRealWorldM3PerSec / $baseUnboostedM3PerSec
    
    # Define fixed tier ranges based on REAL-WORLD BASE m3/s values
    # Scale the original tier ranges by the boost multiplier
    $tierRanges = @{
        'S' = @{ Min = 3.76188 * $boostMultiplier; Max = 3.97 * $boostMultiplier }
        'A' = @{ Min = 3.55376 * $boostMultiplier; Max = 3.76188 * $boostMultiplier }
        'B' = @{ Min = 3.34564 * $boostMultiplier; Max = 3.55376 * $boostMultiplier }
        'C' = @{ Min = 3.13752 * $boostMultiplier; Max = 3.34564 * $boostMultiplier }
        'D' = @{ Min = 2.92940 * $boostMultiplier; Max = 3.13752 * $boostMultiplier }
        'E' = @{ Min = 2.72128 * $boostMultiplier; Max = 2.92940 * $boostMultiplier }
        'F' = @{ Min = $worstRealWorldM3PerSec; Max = 2.72 * $boostMultiplier }
    }
    
    return @{
        BaseRealWorldM3PerSec = $baseRealWorldM3PerSec
        BaseRealWorldBasePlusCritsM3PerSec = $baseRealWorldBasePlusCritsM3PerSec
        BaseRealWorldEffectiveM3PerSec = $baseRealWorldEffectiveM3PerSec
        BestRealWorldM3PerSec = $bestRealWorldM3PerSec
        BestRealWorldBasePlusCritsM3PerSec = $bestRealWorldBasePlusCritsM3PerSec
        BestRealWorldEffectiveM3PerSec = $bestRealWorldEffectiveM3PerSec
        WorstRealWorldM3PerSec = $worstRealWorldM3PerSec
        WorstRealWorldEffectiveM3PerSec = $worstRealWorldEffectiveM3PerSec
        TierRanges = $tierRanges
        BoostMultiplier = $boostMultiplier
    }
}

# Function to display roll analysis
function Show-RollAnalysis {
    param($currentStats, $baseStats, $mutations, $rollRanges)
    
    # Analyze current roll
    $analysis = Analyze-Roll $currentStats $baseStats $mutations
    
    Clear-Host
    Write-Host ""
    Write-Host ("=" * 80)
    Write-Host "EVE Online Modulated Strip Miner II - Real-World Performance Analyzer"
    Write-Host ("=" * 80)
    Write-Host ""
    Write-Host "Configuration: Max Skills + Rorqual Industrial Core Boosts" -ForegroundColor Cyan
    Write-Host ""
    
    Write-Host "Current Item Stats (Base Module):"
    Write-Host ("  Mining Amount: {0:N1} m3" -f $currentStats.MiningAmount)
    Write-Host ("  Activation Time: {0:N1} s" -f $currentStats.ActivationTime)
    Write-Host ("  Critical Success Chance: {0:P2}" -f $currentStats.CriticalSuccessChance)
    Write-Host ("  Critical Success Bonus Yield: {0:P0}" -f $currentStats.CriticalSuccessBonusYield)
    Write-Host ("  Residue Probability: {0:P2}" -f $currentStats.ResidueProbability)
    Write-Host ("  Residue Volume Multiplier: {0:N3} x" -f $currentStats.ResidueVolumeMultiplier)
    Write-Host ""
    
    Write-Host ("=" * 80)
    Write-Host "REAL-WORLD PERFORMANCE ANALYSIS"
    Write-Host ("=" * 80)
    Write-Host ""
    
    # Calculate mutation percentages
    $miningMut = (($analysis.Stats.MiningAmount / $baseStats.MiningAmount) - 1) * 100
    $timeMut = (($analysis.Stats.ActivationTime / $baseStats.ActivationTime) - 1) * 100
    $critChanceMut = if ($baseStats.CriticalSuccessChance -gt 0) { (($analysis.Stats.CriticalSuccessChance / $baseStats.CriticalSuccessChance) - 1) * 100 } else { 0 }
    $critBonusMut = (($analysis.Stats.CriticalSuccessBonusYield / $baseStats.CriticalSuccessBonusYield) - 1) * 100
    $residueProbMut = (($analysis.Stats.ResidueProbability / $baseStats.ResidueProbability) - 1) * 100
    $residueMultMut = (($analysis.Stats.ResidueVolumeMultiplier / $baseStats.ResidueVolumeMultiplier) - 1) * 100
    
    Write-Host "Applied Mutations:"
    
    $miningColor = if ($miningMut -gt 0.1) { 'Green' } elseif ($miningMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  Mining Amount: ") -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $miningMut) -ForegroundColor $miningColor
    
    $timeColor = if ($timeMut -lt -0.1) { 'Green' } elseif ($timeMut -gt 0.1) { 'Red' } else { 'White' }
    Write-Host ("  Activation Time: ") -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $timeMut) -ForegroundColor $timeColor
    
    $critChanceColor = if ($critChanceMut -gt 0.1) { 'Green' } elseif ($critChanceMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  Critical Success Chance: ") -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $critChanceMut) -ForegroundColor $critChanceColor
    
    $critBonusColor = if ($critBonusMut -gt 0.1) { 'Green' } elseif ($critBonusMut -lt -0.1) { 'Red' } else { 'White' }
    Write-Host ("  Critical Success Bonus Yield: ") -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $critBonusMut) -ForegroundColor $critBonusColor
    
    $residueProbColor = if ($residueProbMut -lt -0.1) { 'Green' } elseif ($residueProbMut -gt 0.1) { 'Red' } else { 'White' }
    Write-Host ("  Residue Probability: ") -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $residueProbMut) -ForegroundColor $residueProbColor
    
    $residueMultColor = if ($residueMultMut -lt -0.1) { 'Green' } elseif ($residueMultMut -gt 0.1) { 'Red' } else { 'White' }
    Write-Host ("  Residue Volume Multiplier: ") -NoNewline
    Write-Host ("{0:+0.0;-0.0;+0.0}%" -f $residueMultMut) -ForegroundColor $residueMultColor
    Write-Host ""
    
    Write-Host "Real-World Performance Metrics (with Max Skills + Rorqual Boosts):"
    Write-Host ("{0,-30} {1,-25} {2,-25}" -f "Metric", "Base (Boosted)", "Rolled (Boosted)")
    Write-Host ("-" * 80)
    
    # Real-World Base M3/sec
    $realWorldBaseM3Pct = if ($rollRanges.BaseRealWorldM3PerSec -gt 0) { (($analysis.RealWorldM3PerSec / $rollRanges.BaseRealWorldM3PerSec) - 1) * 100 } else { 0 }
    $realWorldBaseM3Color = if ($realWorldBaseM3Pct -gt 1) { 'Green' } elseif ($realWorldBaseM3Pct -lt -1) { 'Red' } else { 'White' }
    Write-Host ("{0,-30} {1,-25} {2,-25}" -f "Real-World Base M3/sec", "$($rollRanges.BaseRealWorldM3PerSec.ToString('N2')) m3/s", "$($analysis.RealWorldM3PerSec.ToString('N2')) m3/s") -NoNewline
    Write-Host (" ({0:+0.0;-0.0;+0.0}%)" -f $realWorldBaseM3Pct) -ForegroundColor $realWorldBaseM3Color
    
    # Real-World Base + Crits M3/sec (no residue)
    $realWorldBasePlusCritsM3Pct = if ($rollRanges.BaseRealWorldBasePlusCritsM3PerSec -gt 0) { (($analysis.RealWorldBasePlusCritsM3PerSec / $rollRanges.BaseRealWorldBasePlusCritsM3PerSec) - 1) * 100 } else { 0 }
    $realWorldBasePlusCritsM3Color = if ($realWorldBasePlusCritsM3Pct -gt 1) { 'Green' } elseif ($realWorldBasePlusCritsM3Pct -lt -1) { 'Red' } else { 'White' }
    Write-Host ("{0,-30} {1,-25} {2,-25}" -f "Real-World Base + Crits M3/sec", "$($rollRanges.BaseRealWorldBasePlusCritsM3PerSec.ToString('N2')) m3/s", "$($analysis.RealWorldBasePlusCritsM3PerSec.ToString('N2')) m3/s") -NoNewline
    Write-Host (" ({0:+0.0;-0.0;+0.0}%)" -f $realWorldBasePlusCritsM3Pct) -ForegroundColor $realWorldBasePlusCritsM3Color
    
    # Real-World Effective M3/sec
    $realWorldEffM3Pct = if ($rollRanges.BaseRealWorldEffectiveM3PerSec -gt 0) { (($analysis.RealWorldEffectiveM3PerSec / $rollRanges.BaseRealWorldEffectiveM3PerSec) - 1) * 100 } else { 0 }
    $realWorldEffM3Color = if ($realWorldEffM3Pct -gt 1) { 'Green' } elseif ($realWorldEffM3Pct -lt -1) { 'Red' } else { 'White' }
    Write-Host ("{0,-30} {1,-25} {2,-25}" -f "Real-World Effective M3/sec", "$($rollRanges.BaseRealWorldEffectiveM3PerSec.ToString('N2')) m3/s", "$($analysis.RealWorldEffectiveM3PerSec.ToString('N2')) m3/s") -NoNewline
    Write-Host (" ({0:+0.0;-0.0;+0.0}%)" -f $realWorldEffM3Pct) -ForegroundColor $realWorldEffM3Color
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
    if ($analysis.Tier -eq 'S') {
        $tierRangeStr = "{0:N2}-{1:N2}+ m³/s" -f $tierRange.Min, $tierRange.Max
    } elseif ($analysis.Tier -eq 'F') {
        $tierRangeStr = "<{0:N2} m³/s" -f $tierRange.Max
    } else {
        $tierRangeStr = "{0:N2}-{1:N2} m³/s" -f $tierRange.Min, $tierRange.Max
    }
    
    Write-Host ("Tier: ") -NoNewline
    Write-Host ("{0}" -f $analysis.Tier) -ForegroundColor $tierColor
    Write-Host (" ({0})" -f $tierRangeStr) -ForegroundColor $tierColor
    Write-Host ""
    
    Write-Host ("=" * 80)
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "Monitoring clipboard... Last update: $timestamp (Press Ctrl+C to exit)"
    Write-Host ("=" * 80)
}

# Function to analyze a specific roll
function Analyze-Roll {
    param($stats, $baseStats, $mutations)
    
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
    
    # Calculate real-world m3/sec (with all bonuses)
    $realWorldM3PerSec = Calculate-RealWorldBaseM3PerSec $mutatedStats.MiningAmount $mutatedStats.ActivationTime
    $realWorldBasePlusCritsM3PerSec = Calculate-RealWorldBasePlusCritsM3PerSec `
        $mutatedStats.MiningAmount `
        $mutatedStats.ActivationTime `
        $mutatedStats.CriticalSuccessChance `
        $mutatedStats.CriticalSuccessBonusYield
    $realWorldEffectiveM3PerSec = Calculate-RealWorldEffectiveM3PerSec `
        $mutatedStats.MiningAmount `
        $mutatedStats.ActivationTime `
        $mutatedStats.CriticalSuccessChance `
        $mutatedStats.CriticalSuccessBonusYield `
        $mutatedStats.ResidueProbability `
        $mutatedStats.ResidueVolumeMultiplier
    
    # Determine tier based on REAL-WORLD BASE m3/sec
    $rollRanges = Generate-RollRanges $baseStats $mutations
    $tier = "F"
    
    # Check tiers from highest (S) to lowest (F)
    if ($realWorldM3PerSec -ge $rollRanges.TierRanges['S'].Min) {
        $tier = 'S'
    }
    elseif ($realWorldM3PerSec -ge $rollRanges.TierRanges['A'].Min -and $realWorldM3PerSec -lt $rollRanges.TierRanges['A'].Max) {
        $tier = 'A'
    }
    elseif ($realWorldM3PerSec -ge $rollRanges.TierRanges['B'].Min -and $realWorldM3PerSec -lt $rollRanges.TierRanges['B'].Max) {
        $tier = 'B'
    }
    elseif ($realWorldM3PerSec -ge $rollRanges.TierRanges['C'].Min -and $realWorldM3PerSec -lt $rollRanges.TierRanges['C'].Max) {
        $tier = 'C'
    }
    elseif ($realWorldM3PerSec -ge $rollRanges.TierRanges['D'].Min -and $realWorldM3PerSec -lt $rollRanges.TierRanges['D'].Max) {
        $tier = 'D'
    }
    elseif ($realWorldM3PerSec -ge $rollRanges.TierRanges['E'].Min -and $realWorldM3PerSec -lt $rollRanges.TierRanges['E'].Max) {
        $tier = 'E'
    }
    elseif ($realWorldM3PerSec -lt $rollRanges.TierRanges['F'].Max) {
        $tier = 'F'
    }
    else {
        $tier = 'F'
    }
    
    return @{
        Stats = $mutatedStats
        RealWorldM3PerSec = $realWorldM3PerSec
        RealWorldBasePlusCritsM3PerSec = $realWorldBasePlusCritsM3PerSec
        RealWorldEffectiveM3PerSec = $realWorldEffectiveM3PerSec
        Tier = $tier
    }
}

# Main execution
Write-Host ""
Write-Host ("=" * 80)
Write-Host "EVE Online Modulated Strip Miner II - Real-World Performance Analyzer"
Write-Host ("=" * 80)
Write-Host ""
Write-Host "This tool calculates performance with MAX SKILLS and RORQUAL BOOSTS applied."
Write-Host "It will monitor your clipboard and update automatically when you copy new item stats."
Write-Host "Press Ctrl+C to exit."
Write-Host ""

# Generate roll ranges
$rollRanges = Generate-RollRanges $baseStats $mutations

Write-Host "Configuration:" -ForegroundColor Cyan
Write-Host ("  Skills: Mining {0}, Astrogeology {1}, Exhumer {2}" -f $miningSkills.Mining, $miningSkills.Astrogeology, $miningSkills.Exhumer)
Write-Host ("  Ship Role Bonus: {0:P0}" -f ($shipRoleBonus - 1))
Write-Host ("  Mining Laser Upgrade II: {0:P0} (3 modules)" -f ($moduleBonus - 1))
Write-Host ("  Mining Crystal (Type B II): {0:P0}" -f ($miningCrystalBonus - 1))
Write-Host ("  Rorqual Industrial Core: {0:P0} yield, {1:P0} cycle time reduction" -f ($rorqualBoosts.IndustrialCoreYield - 1), (1 - $rorqualBoosts.IndustrialCoreCycleTime))
Write-Host ("  Mining Foreman Burst: {0:P0} yield" -f ($rorqualBoosts.MiningForemanBurstYield - 1))
Write-Host ""
Write-Host ("Base Real-World Performance: {0:N2} m³/s (boosted)" -f $rollRanges.BaseRealWorldM3PerSec) -ForegroundColor Green
Write-Host ("Boost Multiplier: {0:N2}x" -f $rollRanges.BoostMultiplier) -ForegroundColor Cyan
Write-Host ""

# Track last clipboard content
$lastClipboardText = $null
$firstRun = $true
$lastProcessedTime = Get-Date

# Main monitoring loop
while ($true) {
    try {
        $clipboardText = $null
        try {
            $clipboardText = Get-Clipboard -Format Text -ErrorAction Stop
        } catch {
            try {
                $clipboardText = Get-Clipboard -ErrorAction Stop
            } catch {
                throw
            }
        }
        
        if ($null -eq $clipboardText) {
            Start-Sleep -Milliseconds 300
            continue
        }
        
        if ($clipboardText -isnot [string]) {
            $clipboardText = $clipboardText.ToString()
        }
        
        if ([string]::IsNullOrWhiteSpace($clipboardText)) {
            Start-Sleep -Milliseconds 300
            continue
        }
        
        if ($clipboardText -ne $lastClipboardText -and $clipboardText.Length -gt 0) {
            $lastClipboardText = $clipboardText
            $currentTime = Get-Date
            
            $parsedStats = Parse-ItemStats $clipboardText
            
            $currentStats = $baseStats.Clone()
            $parsedCount = 0
            foreach ($key in $parsedStats.Keys) {
                $currentStats[$key] = $parsedStats[$key]
                $parsedCount++
            }
            
            if ($parsedCount -gt 0) {
                Show-RollAnalysis $currentStats $baseStats $mutations $rollRanges
                $firstRun = $false
                $lastProcessedTime = $currentTime
            } elseif ($firstRun) {
                Clear-Host
                Write-Host ""
                Write-Host ("=" * 80)
                Write-Host "EVE Online Modulated Strip Miner II - Real-World Performance Analyzer"
                Write-Host ("=" * 80)
                Write-Host ""
                Write-Host "Waiting for item stats in clipboard..."
                Write-Host ""
                Write-Host "Instructions:"
                Write-Host "  1. Copy item stats from EVE Online (Ctrl+C on item info window)"
                Write-Host "  2. The analysis will appear automatically"
                Write-Host "  3. Copy new items to see updated analysis"
                Write-Host ""
                Write-Host "Press Ctrl+C to exit."
                Write-Host ("=" * 80)
                $firstRun = $false
            }
        }
        
        Start-Sleep -Milliseconds 300
    } catch {
        $errorMsg = $_.Exception.Message
        if ($firstRun) {
            Clear-Host
            Write-Host ""
            Write-Host ("=" * 80)
            Write-Host "EVE Online Modulated Strip Miner II - Real-World Performance Analyzer"
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

