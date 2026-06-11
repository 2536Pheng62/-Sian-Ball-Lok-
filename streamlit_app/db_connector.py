import sqlite3
import os
from datetime import datetime

DB_FILE = 'sian_ball_lok_v2.db'

def get_connection():
    """Returns a connection to the SQLite database."""
    conn = sqlite3.connect(DB_FILE)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    """Initializes the SQLite database tables and seeds matches on first run."""
    conn = get_connection()
    cursor = conn.cursor()
    
    # 1. Users Table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS users (
            user_id INTEGER PRIMARY KEY AUTOINCREMENT,
            display_name TEXT UNIQUE NOT NULL,
            color_hex TEXT DEFAULT '#FFC107',
            pin TEXT DEFAULT '',
            champion_guess TEXT DEFAULT ''
        )
    ''')
    
    # 2. Matches Table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS matches (
            match_id INTEGER PRIMARY KEY,
            team_a TEXT NOT NULL,
            team_a_flag TEXT DEFAULT '',
            team_b TEXT NOT NULL,
            team_b_flag TEXT DEFAULT '',
            stage TEXT DEFAULT '',
            status TEXT DEFAULT 'pending',
            actual_result TEXT DEFAULT NULL,
            team_a_score INTEGER DEFAULT -1,
            team_b_score INTEGER DEFAULT -1,
            kickoff_time TEXT NOT NULL,
            locked INTEGER DEFAULT 0
        )
    ''')
    
    # 3. Predictions Table
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS predictions (
            prediction_id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            match_id INTEGER NOT NULL,
            predicted_result TEXT NOT NULL, -- 'team_a_win', 'team_b_win', 'draw'
            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
            FOREIGN KEY (match_id) REFERENCES matches(match_id),
            UNIQUE(user_id, match_id)
        )
    ''')
    
    conn.commit()
    
    # Seed Matches on first initialization
    cursor.execute("SELECT COUNT(*) FROM matches")
    count = cursor.fetchone()[0]
    
    if count == 0:
        initial_matches = [
            (1, "เม็กซิโก", "🇲🇽", "ออสเตรเลีย", "🇦🇺", "รอบแบ่งกลุ่ม (กลุ่ม A)", "pending", None, -1, -1, "2026-06-12T02:00:00", 0),
            (2, "แคนาดา", "🇨🇦", "ญี่ปุ่น", "🇯🇵", "รอบแบ่งกลุ่ม (กลุ่ม B)", "pending", None, -1, -1, "2026-06-13T02:00:00", 0),
            (3, "สหรัฐอเมริกา", "🇺🇸", "โมร็อกโก", "🇲🇦", "รอบแบ่งกลุ่ม (กลุ่ม D)", "pending", None, -1, -1, "2026-06-13T08:00:00", 0),
            (4, "อาร์เจนตินา", "🇦🇷", "ซาอุดีอาระเบีย", "🇸🇦", "รอบแบ่งกลุ่ม (กลุ่ม F)", "pending", None, -1, -1, "2026-06-14T05:00:00", 0),
            (5, "ฝรั่งเศส", "🇫🇷", "ออสเตรีย", "🇦🇹", "รอบแบ่งกลุ่ม (กลุ่ม I)", "pending", None, -1, -1, "2026-06-15T02:00:00", 0),
            (6, "อังกฤษ", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "เกาหลีใต้", "🇰🇷", "รอบแบ่งกลุ่ม (กลุ่ม C)", "pending", None, -1, -1, "2026-06-16T02:00:00", 0),
            (7, "บราซิล", "🇧🇷", "เซอร์เบีย", "🇷🇸", "รอบแบ่งกลุ่ม (กลุ่ม G)", "pending", None, -1, -1, "2026-06-17T08:00:00", 0),
            (8, "สเปน", "🇪🇸", "สวิตเซอร์แลนด์", "🇨🇭", "รอบแบ่งกลุ่ม (กลุ่ม K)", "pending", None, -1, -1, "2026-06-18T05:00:00", 0),
            (9, "แคนาดา", "🇨🇦", "อาร์เจนตินา", "🇦🇷", "รอบ 32 ทีมสุดท้าย", "pending", None, -1, -1, "2026-06-30T02:00:00", 0),
            (10, "สหรัฐอเมริกา", "🇺🇸", "ฝรั่งเศส", "🇫🇷", "รอบ 16 ทีมสุดท้าย", "pending", None, -1, -1, "2026-07-06T02:00:00", 0),
            (11, "บราซิล", "🇧🇷", "เยอรมนี", "🇩🇪", "รอบรองชนะเลิศ", "pending", None, -1, -1, "2026-07-15T02:00:00", 0),
            (12, "อังกฤษ", "🏴󠁧󠁢󠁥󠁮󠁧󠁿", "โปรตุเกส", "🇵🇹", "รอบรองชนะเลิศ", "pending", None, -1, -1, "2026-07-16T02:00:00", 0),
            (13, "อาร์เจนตินา", "🇦🇷", "ฝรั่งเศส", "🇫🇷", "รอบชิงชนะเลิศ", "pending", None, -1, -1, "2026-07-20T02:00:00", 0)
        ]
        cursor.executemany('''
            INSERT INTO matches (match_id, team_a, team_a_flag, team_b, team_b_flag, stage, status, actual_result, team_a_score, team_b_score, kickoff_time, locked)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ''', initial_matches)
        
        # Seed default users
        cursor.execute("INSERT OR IGNORE INTO users (user_id, display_name, color_hex, pin, champion_guess) VALUES (1, 'คนรักบอล', '#FFC107', '', 'อาร์เจนตินา')")
        cursor.execute("INSERT OR IGNORE INTO users (user_id, display_name, color_hex, pin, champion_guess) VALUES (2, 'สมชาย', '#00E676', '1234', 'ฝรั่งเศส')")
        
        conn.commit()
    
    conn.close()

# --- Users Query Methods ---
def get_users():
    conn = get_connection()
    users = conn.execute("SELECT * FROM users ORDER BY user_id ASC").fetchall()
    conn.close()
    return [dict(u) for u in users]

def add_user(display_name, color_hex='#FFC107', pin='', champion_guess=''):
    conn = get_connection()
    cursor = conn.cursor()
    try:
        cursor.execute('''
            INSERT INTO users (display_name, color_hex, pin, champion_guess)
            VALUES (?, ?, ?, ?)
        ''', (display_name, color_hex, pin, champion_guess))
        conn.commit()
        new_id = cursor.lastrowid
        return new_id
    except sqlite3.IntegrityError:
        return None
    finally:
        conn.close()

def delete_user(user_id):
    conn = get_connection()
    conn.execute("DELETE FROM users WHERE user_id = ?", (user_id,))
    # Predictions get cascade deleted
    conn.commit()
    conn.close()

def update_user_champion(user_id, champion):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT champion_guess FROM users WHERE user_id = ?", (user_id,))
    row = cursor.fetchone()
    if row and row['champion_guess'] and row['champion_guess'].strip() != "":
        conn.close()
        return False
    conn.execute("UPDATE users SET champion_guess = ? WHERE user_id = ?", (champion, user_id))
    conn.commit()
    conn.close()
    return True

# --- Matches Query Methods ---
def get_matches():
    conn = get_connection()
    matches = conn.execute("SELECT * FROM matches ORDER BY match_id ASC").fetchall()
    conn.close()
    return [dict(m) for m in matches]

def update_match_result(match_id, score_a, score_b, result, status='finished'):
    conn = get_connection()
    conn.execute('''
        UPDATE matches 
        SET team_a_score = ?, team_b_score = ?, actual_result = ?, status = ?
        WHERE match_id = ?
    ''', (score_a, score_b, result, status, match_id))
    conn.commit()
    conn.close()

def clear_match_result(match_id):
    conn = get_connection()
    conn.execute('''
        UPDATE matches 
        SET team_a_score = -1, team_b_score = -1, actual_result = NULL, status = 'pending', locked = 0
        WHERE match_id = ?
    ''', (match_id,))
    conn.commit()
    conn.close()

def toggle_match_lock(match_id):
    conn = get_connection()
    conn.execute('UPDATE matches SET locked = 1 - locked WHERE match_id = ?', (match_id,))
    conn.commit()
    conn.close()

def update_match_kickoff(match_id, kickoff_time):
    conn = get_connection()
    conn.execute('UPDATE matches SET kickoff_time = ? WHERE match_id = ?', (kickoff_time, match_id))
    conn.commit()
    conn.close()

def reset_all_matches():
    conn = get_connection()
    conn.execute('''
        UPDATE matches 
        SET team_a_score = -1, team_b_score = -1, actual_result = NULL, status = 'pending', locked = 0
    ''')
    conn.commit()
    conn.close()

def simulate_tournament():
    import random
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT match_id, stage FROM matches")
    rows = cursor.fetchall()
    for row in rows:
        match_id = row['match_id']
        stage = row['stage']
        score_a = random.randint(0, 4)
        score_b = random.randint(0, 4)
        if score_a > score_b:
            result = 'team_a_win'
        elif score_a < score_b:
            result = 'team_b_win'
        else:
            # Check if it's knockout stage
            if "รอบแบ่งกลุ่ม" not in stage:
                result = 'team_a_win' if random.random() > 0.5 else 'team_b_win'
            else:
                result = 'draw'
        cursor.execute('''
            UPDATE matches 
            SET team_a_score = ?, team_b_score = ?, actual_result = ?, status = 'finished'
            WHERE match_id = ?
        ''', (score_a, score_b, result, match_id))
    conn.commit()
    conn.close()

# --- Predictions Query Methods ---
def get_predictions():
    conn = get_connection()
    preds = conn.execute("SELECT * FROM predictions").fetchall()
    conn.close()
    return [dict(p) for p in preds]

def submit_prediction(user_id, match_id, predicted_result):
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("SELECT predicted_result FROM predictions WHERE user_id = ? AND match_id = ?", (user_id, match_id))
    row = cursor.fetchone()
    if row and row['predicted_result'] and row['predicted_result'] != "PENDING":
        conn.close()
        return False
    cursor.execute('''
        INSERT INTO predictions (user_id, match_id, predicted_result)
        VALUES (?, ?, ?)
        ON CONFLICT(user_id, match_id) DO UPDATE SET predicted_result=excluded.predicted_result
    ''', (user_id, match_id, predicted_result))
    conn.commit()
    conn.close()
    return True

# --- Cloud Database Hook placeholders ---
# You can connect to Supabase/Firestore by calling these variables from environment config
SUPABASE_URL = os.environ.get("SUPABASE_URL")
SUPABASE_KEY = os.environ.get("SUPABASE_KEY")

def sync_to_cloud():
    """Placeholder to sync SQLite states to Supabase/Firestore if credentials are set."""
    if SUPABASE_URL and SUPABASE_KEY:
        # Code to write state to cloud tables
        pass
    else:
        pass

def get_simulated_score(match_id, elapsed_minutes):
    """Returns deterministic goal count based on elapsed minutes and match id."""
    # Deterministic final score
    team_a_final = (match_id * 3 + 1) % 4
    team_b_final = (match_id * 7 + 2) % 4
    
    # Deterministic scoring times
    team_a_minutes = []
    if team_a_final >= 1:
        team_a_minutes.append((match_id * 11) % 40)
    if team_a_final >= 2:
        team_a_minutes.append(45 + (match_id * 17) % 40)
    if team_a_final >= 3:
        team_a_minutes.append(75 + (match_id * 23) % 15)
        
    team_b_minutes = []
    if team_b_final >= 1:
        team_b_minutes.append((match_id * 13) % 45)
    if team_b_final >= 2:
        team_b_minutes.append(45 + (match_id * 19) % 45)
    if team_b_final >= 3:
        team_b_minutes.append(75 + (match_id * 29) % 15)
        
    goals_a = sum(1 for m in team_a_minutes if m <= elapsed_minutes) if elapsed_minutes >= 0 else 0
    goals_b = sum(1 for m in team_b_minutes if m <= elapsed_minutes) if elapsed_minutes >= 0 else 0
    
    return min(goals_a, team_a_final), min(goals_b, team_b_final)

def auto_update_live_matches(simulated_time_str=None):
    """Automatically updates ongoing and completed match scores and statuses based on current time."""
    conn = get_connection()
    cursor = conn.cursor()
    
    cursor.execute("SELECT match_id, kickoff_time, status FROM matches")
    matches = cursor.fetchall()
    
    if simulated_time_str:
        ref_time = datetime.fromisoformat(simulated_time_str)
    else:
        ref_time = datetime.now()
        
    for m in matches:
        match_id = m['match_id']
        kickoff_time = datetime.fromisoformat(m['kickoff_time'])
        
        delta = ref_time - kickoff_time
        elapsed_minutes = delta.total_seconds() / 60.0
        
        if elapsed_minutes >= 0:
            if elapsed_minutes >= 120:
                # Match is completed (finished)
                goals_a, goals_b = get_simulated_score(match_id, 120)
                res = 'draw'
                if goals_a > goals_b:
                    res = 'team_a_win'
                elif goals_a < goals_b:
                    res = 'team_b_win'
                cursor.execute('''
                    UPDATE matches 
                    SET team_a_score = ?, team_b_score = ?, actual_result = ?, status = 'finished', locked = 1
                    WHERE match_id = ? AND status != 'finished'
                ''', (goals_a, goals_b, res, match_id))
            else:
                # Match is currently live (ongoing)
                goals_a, goals_b = get_simulated_score(match_id, elapsed_minutes)
                res = 'draw'
                if goals_a > goals_b:
                    res = 'team_a_win'
                elif goals_a < goals_b:
                    res = 'team_b_win'
                cursor.execute('''
                    UPDATE matches 
                    SET team_a_score = ?, team_b_score = ?, actual_result = ?, status = 'live', locked = 1
                    WHERE match_id = ? AND status != 'finished'
                ''', (goals_a, goals_b, res, match_id))
        else:
            # Match is in the future
            cursor.execute('''
                UPDATE matches
                SET team_a_score = -1, team_b_score = -1, actual_result = NULL, status = 'pending', locked = 0
                WHERE match_id = ? AND status != 'pending'
            ''', (match_id,))
            
    conn.commit()
    conn.close()

def sync_scores_from_json(matches_data):
    """Syncs actual match scores, teams, stages, flags, kickoff times from a JSON structure.
    Also inserts matches if they do not exist.
    """
    conn = get_connection()
    cursor = conn.cursor()
    for m in matches_data:
        match_id = m.get('match_id') or m.get('id')
        if match_id is None:
            continue
            
        team_a = m.get('team_a') or m.get('teamA')
        team_b = m.get('team_b') or m.get('teamB')
        team_a_flag = m.get('team_a_flag') or m.get('teamAFlag')
        team_b_flag = m.get('team_b_flag') or m.get('teamBFlag')
        stage = m.get('stage')
        kickoff_time = m.get('kickoff_time') or m.get('kickoffTime')
        
        score_a = m.get('team_a_score') if m.get('team_a_score') is not None else m.get('score_a')
        score_b = m.get('team_b_score') if m.get('team_b_score') is not None else m.get('score_b')
        status = m.get('status', 'finished')
        result = m.get('actual_result') or m.get('result')
        
        # Check if match already exists
        cursor.execute("SELECT * FROM matches WHERE match_id = ?", (match_id,))
        current = cursor.fetchone()
        
        if current:
            # Update existing match details
            new_team_a = team_a if team_a is not None else current['team_a']
            new_team_b = team_b if team_b is not None else current['team_b']
            new_team_a_flag = team_a_flag if team_a_flag is not None else current['team_a_flag']
            new_team_b_flag = team_b_flag if team_b_flag is not None else current['team_b_flag']
            new_stage = stage if stage is not None else current['stage']
            new_kickoff_time = kickoff_time if kickoff_time is not None else current['kickoff_time']
            
            new_score_a = score_a if score_a is not None else current['team_a_score']
            new_score_b = score_b if score_b is not None else current['team_b_score']
            new_status = status if status is not None else current['status']
            
            # Resolve actual result if scores are set
            new_result = result
            if not new_result and new_score_a >= 0 and new_score_b >= 0:
                if new_score_a > new_score_b:
                    new_result = 'team_a_win'
                elif new_score_a < new_score_b:
                    new_result = 'team_b_win'
                else:
                    new_result = 'draw'
            elif new_result is None:
                new_result = current['actual_result']
                
            locked = 1 if new_status in ('finished', 'live') else current['locked']
            
            cursor.execute('''
                UPDATE matches 
                SET team_a = ?, team_b = ?, team_a_flag = ?, team_b_flag = ?, stage = ?, 
                    kickoff_time = ?, team_a_score = ?, team_b_score = ?, actual_result = ?, 
                    status = ?, locked = ?
                WHERE match_id = ?
            ''', (new_team_a, new_team_b, new_team_a_flag, new_team_b_flag, new_stage, 
                  new_kickoff_time, new_score_a, new_score_b, new_result, new_status, locked, match_id))
        else:
            # Insert new match
            if team_a and team_b and kickoff_time:
                new_team_a_flag = team_a_flag if team_a_flag else ""
                new_team_b_flag = team_b_flag if team_b_flag else ""
                new_stage = stage if stage else ""
                new_score_a = score_a if score_a is not None else -1
                new_score_b = score_b if score_b is not None else -1
                
                new_result = result
                if not new_result and new_score_a >= 0 and new_score_b >= 0:
                    if new_score_a > new_score_b:
                        new_result = 'team_a_win'
                    elif new_score_a < new_score_b:
                        new_result = 'team_b_win'
                    else:
                        new_result = 'draw'
                
                locked = 1 if status in ('finished', 'live') else 0
                cursor.execute('''
                    INSERT INTO matches (match_id, team_a, team_a_flag, team_b, team_b_flag, stage, status, actual_result, team_a_score, team_b_score, kickoff_time, locked)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ''', (match_id, team_a, new_team_a_flag, team_b, new_team_b_flag, new_stage, status, new_result, new_score_a, new_score_b, kickoff_time, locked))
                
    conn.commit()
    conn.close()
