import os
import json
import urllib.request
from datetime import datetime, timedelta
import db_connector as db

# Team Name and Flag Mapping dictionaries
TEAM_MAPPING = {
    "Mexico": "เม็กซิโก",
    "Australia": "ออสเตรเลีย",
    "Canada": "แคนาดา",
    "Japan": "ญี่ปุ่น",
    "United States": "สหรัฐอเมริกา",
    "Morocco": "โมร็อกโก",
    "Argentina": "อาร์เจนตินา",
    "Saudi Arabia": "ซาอุดีอาระเบีย",
    "France": "ฝรั่งเศส",
    "Austria": "ออสเตรีย",
    "England": "อังกฤษ",
    "South Korea": "เกาหลีใต้",
    "Brazil": "บราซิล",
    "Serbia": "เซอร์เบีย",
    "Spain": "สเปน",
    "Switzerland": "สวิตเซอร์แลนด์",
    "Germany": "เยอรมนี",
    "Portugal": "โปรตุเกส",
    "Croatia": "โครเอเชีย",
    "Netherlands": "เนเธอร์แลนด์",
    "Italy": "อิตาลี",
    "Belgium": "เบลเยียม",
    "Uruguay": "อุรุกวัย",
    "Senegal": "เซเนกัล",
    "Ecuador": "เอกวาดอร์",
    "Denmark": "เดนมาร์ก",
    "Poland": "โปแลนด์",
    "Costa Rica": "คอสตาริกา",
    "Cameroon": "แคเมอรูน",
    "Ghana": "กานา",
    "Tunisia": "ตูนิเซีย",
    "Qatar": "กาตาร์",
    "Iran": "อิหร่าน",
    "Wales": "เวลส์",
    "Ukraine": "ยูเครน",
    "Scotland": "สกอตแลนด์",
    "Peru": "เปรู",
    "Colombia": "โคลอมเบีย",
    "Sweden": "สวีเดน",
    "Turkey": "ตุรกี",
    "Chile": "ชิลี",
    "Nigeria": "ไนจีเรีย",
    "Algeria": "แอลจีเรีย",
    "Egypt": "อียิปต์"
}

FLAG_MAPPING = {
    "เม็กซิโก": "🇲🇽",
    "ออสเตรเลีย": "🇦🇺",
    "แคนาดา": "🇨🇦",
    "ญี่ปุ่น": "🇯🇵",
    "สหรัฐอเมริกา": "🇺🇸",
    "โมร็อกโก": "🇲🇦",
    "อาร์เจนตินา": "🇦🇷",
    "ซาอุดีอาระเบีย": "🇸🇦",
    "ฝรั่งเศส": "🇫🇷",
    "ออสเตรีย": "🇦🇹",
    "อังกฤษ": "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
    "เกาหลีใต้": "🇰🇷",
    "บราซิล": "🇧🇷",
    "เซอร์เบีย": "🇷🇸",
    "สเปน": "🇪🇸",
    "สวิตเซอร์แลนด์": "🇨🇭",
    "เยอรมนี": "🇩🇪",
    "โปรตุเกส": "🇵🇹",
    "โครเอเชีย": "🇭🇷",
    "เนเธอร์แลนด์": "🇳🇱",
    "อิตาลี": "🇮🇹",
    "เบลเยียม": "🇧🇪",
    "อุรุกวัย": "🇺🇾",
    "เซเนกัล": "🇸🇳",
    "เอกวาดอร์": "🇪🇨",
    "เดนมาร์ก": "🇩🇰",
    "โปแลนด์": "🇵🇱",
    "คอสตาริกา": "🇨🇷",
    "แคเมอรูน": "🇨🇲",
    "กานา": "🇬🇭",
    "ตูนิเซีย": "🇹🇳",
    "กาตาร์": "🇶🇦",
    "อิหร่าน": "🇮🇷",
    "เวลส์": "🏴󠁧󠁢󠁷󠁬󠁳󠁿",
    "ยูเครน": "🇺🇦",
    "สกอตแลนด์": "🏴󠁧󠁢󠁳󠁣󠁴󠁿",
    "เปรู": "🇵🇪",
    "โคลอมเบีย": "🇨🇴",
    "สวีเดน": "🇸🇪",
    "ตุรกี": "🇹🇷",
    "ชิลี": "🇨🇱",
    "ไนจีเรีย": "🇳🇬",
    "แอลจีเรีย": "🇩🇿",
    "อียิปต์": "🇪🇬"
}

def translate_stage(stage, group):
    stage_mapping = {
        "GROUP_STAGE": "รอบแบ่งกลุ่ม",
        "LAST_32": "รอบ 32 ทีมสุดท้าย",
        "LAST_16": "รอบ 16 ทีมสุดท้าย",
        "QUARTER_FINALS": "รอบ 8 ทีมสุดท้าย",
        "SEMI_FINALS": "รอบรองชนะเลิศ",
        "THIRD_PLACE": "รอบชิงอันดับ 3",
        "FINAL": "รอบชิงชนะเลิศ"
    }
    th_stage = stage_mapping.get(stage, stage)
    if stage == "GROUP_STAGE" and group:
        # e.g., GROUP_A -> กลุ่ม A
        group_letter = group.replace("GROUP_", "")
        th_stage = f"{th_stage} (กลุ่ม {group_letter})"
    return th_stage

def convert_utc_to_ict(utc_str):
    """Converts UTC ISO string to Thailand Time (ICT = UTC+7) ISO string."""
    try:
        utc_str = utc_str.replace('Z', '')
        if '.' in utc_str:
            dt = datetime.strptime(utc_str, '%Y-%m-%dT%H:%M:%S.%f')
        else:
            dt = datetime.strptime(utc_str, '%Y-%m-%dT%H:%M:%S')
        ict_dt = dt + timedelta(hours=7)
        return ict_dt.isoformat()
    except Exception as e:
        print(f"Error parsing date {utc_str}: {e}")
        return utc_str

def run_sync(api_token=None):
    """Fetches matches from Football-Data.org and updates the local database and JSON scores file."""
    # Priority for API Token: 
    # 1. Parameter passed
    # 2. Environment variable
    token = api_token or os.environ.get("FOOTBALL_DATA_API_TOKEN")
    
    matches_raw = []
    source = ""

    if token:
        print("Starting sync using live Football-Data.org API...")
        url = "https://api.football-data.org/v4/competitions/WC/matches"
        try:
            req = urllib.request.Request(url)
            req.add_header("X-Auth-Token", token)
            req.add_header("User-Agent", "Mozilla/5.0")
            
            with urllib.request.urlopen(req) as response:
                payload = json.loads(response.read().decode('utf-8'))
                matches_raw = payload.get("matches", [])
                source = "Football-Data API"
                print(f"Successfully fetched {len(matches_raw)} matches from API.")
        except Exception as e:
            print(f"Failed to fetch from API: {e}. Falling back to mock file...")
            token = None  # Force mock fallback

    if not token:
        print("No API Token configured or API request failed. Using mock file fallback...")
        # Resolve mock path
        script_dir = os.path.dirname(os.path.abspath(__file__))
        mock_path = os.path.join(os.path.dirname(script_dir), "mock_football_data_api.json")
        
        if not os.path.exists(mock_path):
            # Try same dir
            mock_path = os.path.join(script_dir, "mock_football_data_api.json")
            
        if os.path.exists(mock_path):
            with open(mock_path, 'r', encoding='utf-8') as f:
                payload = json.load(f)
                matches_raw = payload.get("matches", [])
                source = "mock_football_data_api.json"
                print(f"Successfully loaded {len(matches_raw)} matches from mock file.")
        else:
            print(f"Error: Mock file not found at {mock_path}.")
            return False, "Mock data file not found."

    translated_matches = []
    
    # Status map
    status_mapping = {
        'FINISHED': 'finished',
        'IN_PLAY': 'live',
        'PAUSED': 'live',
        'TIMED': 'pending',
        'SCHEDULED': 'pending'
    }

    for m in matches_raw:
        match_id = m.get("id")
        if not match_id:
            continue
            
        home_team_en = m.get("homeTeam", {}).get("name", "")
        away_team_en = m.get("awayTeam", {}).get("name", "")
        
        # Translate home/away team names
        team_a = TEAM_MAPPING.get(home_team_en, home_team_en)
        team_b = TEAM_MAPPING.get(away_team_en, away_team_en)
        
        # Find flags
        team_a_flag = FLAG_MAPPING.get(team_a, "🏳️")
        team_b_flag = FLAG_MAPPING.get(team_b, "🏳️")
        
        # Stage translation
        stage = translate_stage(m.get("stage", ""), m.get("group", ""))
        
        # Kickoff Time ICT conversion
        kickoff_time = convert_utc_to_ict(m.get("utcDate", ""))
        
        # Score parsing
        score = m.get("score", {})
        full_time = score.get("fullTime", {})
        score_a = full_time.get("home")
        score_b = full_time.get("away")
        
        # Normalize -1 for null scores
        if score_a is None:
            score_a = -1
        if score_b is None:
            score_b = -1
            
        # Status mapping
        raw_status = m.get("status", "")
        status = status_mapping.get(raw_status, "pending")
        
        # Winner Result resolution
        winner = score.get("winner")
        result = None
        if winner == "HOME_TEAM":
            result = "team_a_win"
        elif winner == "AWAY_TEAM":
            result = "team_b_win"
        elif winner == "DRAW":
            result = "draw"
        elif score_a >= 0 and score_b >= 0:
            # Fallback if winner string is null but scores are present
            if score_a > score_b:
                result = "team_a_win"
            elif score_a < score_b:
                result = "team_b_win"
            else:
                result = "draw"
                
        translated_matches.append({
            "match_id": match_id,
            "team_a": team_a,
            "team_a_flag": team_a_flag,
            "team_b": team_b,
            "team_b_flag": team_b_flag,
            "stage": stage,
            "kickoff_time": kickoff_time,
            "team_a_score": score_a,
            "team_b_score": score_b,
            "status": status,
            "actual_result": result
        })

    # Update database
    db.init_db()
    db.sync_scores_from_json(translated_matches)
    print("Database sync completed.")
    
    # Query all matches from database to write to live_scores.json
    db_matches = db.get_matches()
    
    # Save to live_scores.json in the parent directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    root_dir = os.path.dirname(script_dir)
    json_path = os.path.join(root_dir, 'live_scores.json')
    
    try:
        with open(json_path, 'w', encoding='utf-8') as f:
            json.dump({"matches": db_matches}, f, ensure_ascii=False, indent=2)
        print(f"Successfully generated {json_path} for offline clients.")
    except Exception as e:
        print(f"Failed to write live_scores.json: {e}")

    return True, f"Successfully synced from {source} ({len(translated_matches)} matches)."

if __name__ == "__main__":
    success, msg = run_sync()
    print(msg)
