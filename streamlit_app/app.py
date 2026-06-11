import streamlit as st
import pandas as pd
from datetime import datetime
import time
import os
import db_connector as db
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Initialize database
db.init_db()

# Auto-Sync from API/Mock if last sync was > 5 minutes ago
try:
    json_path = "live_scores.json"
    if not os.path.exists(json_path) and os.path.exists(os.path.join("..", "live_scores.json")):
        json_path = os.path.join("..", "live_scores.json")
    
    should_auto_sync = False
    if os.path.exists(json_path):
        mtime = os.path.getmtime(json_path)
        if time.time() - mtime > 300:  # 5 minutes
            should_auto_sync = True
    else:
        should_auto_sync = True
        
    if should_auto_sync:
        import sync_api
        token = os.environ.get("FOOTBALL_DATA_API_TOKEN")
        sync_api.run_sync(token)
except Exception as e:
    pass

# Page configuration
st.set_page_config(
    page_title="เซียนบอลโลก 2026",
    page_icon="🏆",
    layout="wide",
    initial_sidebar_state="expanded"
)

# --- Flag SVGs Dictionary and Helper ---
FLAG_SVGS = {
    "เม็กซิโก": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="10" height="20" fill="#006847"/><rect x="10" width="10" height="20" fill="#fff"/><rect x="20" width="10" height="20" fill="#c8102e"/><circle cx="15" cy="10" r="2" fill="#8c6239"/></svg>""",
    "ออสเตรเลีย": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="20" fill="#00008b"/><path d="M0 0h12v8H0z" fill="#fff"/><path d="M0 0l12 8M0 8l12-8" stroke="#ff0000" stroke-width="1.5"/><path d="M0 4h12M6 0v8" stroke="#ff0000" stroke-width="2.5" stroke-linecap="round"/><circle cx="22.5" cy="5" r="1.5" fill="#fff"/><circle cx="18" cy="12" r="1.5" fill="#fff"/><circle cx="27" cy="12" r="1.5" fill="#fff"/><circle cx="22.5" cy="15" r="1.5" fill="#fff"/><circle cx="22.5" cy="10" r="1.5" fill="#fff"/></svg>""",
    "แคนาดา": """<svg class="flag-svg" viewBox="0 0 30 15" width="24" height="16"><rect width="7.5" height="15" fill="#ff0000"/><rect x="7.5" width="15" height="15" fill="#fff"/><rect x="22.5" width="7.5" height="15" fill="#ff0000"/><path d="M15 4l1 3h3l-2.5 2 1 3-2.5-2-2.5 2 1-3-2.5-2h3z" fill="#ff0000"/></svg>""",
    "ญี่ปุ่น": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16" style="border: 1px solid rgba(255,255,255,0.08)"><rect width="30" height="20" fill="#fff"/><circle cx="15" cy="10" r="6" fill="#bc002d"/></svg>""",
    "สหรัฐอเมริกา": """<svg class="flag-svg" viewBox="0 0 30 16" width="24" height="16"><rect width="30" height="16" fill="#fff"/><path d="M0 0h30v1.23H0zm0 2.46h30v1.23H0zm0 2.46h30v1.23H0zm0 2.46h30v1.23H0zm0 2.46h30v1.23H0zm0 2.46h30v1.23H0zm0 2.46h30v1.23H0z" fill="#b22234"/><rect width="12" height="8.6" fill="#3c3b6e"/><circle cx="2" cy="2" r="0.4" fill="#fff"/><circle cx="4" cy="2" r="0.4" fill="#fff"/><circle cx="6" cy="2" r="0.4" fill="#fff"/><circle cx="8" cy="2" r="0.4" fill="#fff"/><circle cx="10" cy="2" r="0.4" fill="#fff"/><circle cx="3" cy="4" r="0.4" fill="#fff"/><circle cx="5" cy="4" r="0.4" fill="#fff"/><circle cx="7" cy="4" r="0.4" fill="#fff"/><circle cx="9" cy="4" r="0.4" fill="#fff"/></svg>""",
    "โมร็อกโก": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="20" fill="#c8102e"/><polygon points="15,6 17.5,14 11,9 19,9 12.5,14" fill="none" stroke="#006233" stroke-width="1.5"/></svg>""",
    "อาร์เจนตินา": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="6.6" fill="#74acdf"/><rect y="6.6" width="30" height="6.8" fill="#fff"/><rect y="13.4" width="30" height="6.6" fill="#74acdf"/><circle cx="15" cy="10" r="2" fill="#ffb81c"/></svg>""",
    "ซาอุดีอาระเบีย": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="20" fill="#006c35"/><path d="M6 13h18v1H6z" fill="#fff"/><path d="M10 9h10v1H10z" fill="#fff"/></svg>""",
    "ฝรั่งเศส": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="10" height="20" fill="#002395"/><rect x="10" width="10" height="20" fill="#fff"/><rect x="20" width="10" height="20" fill="#ed2939"/></svg>""",
    "ออสเตรีย": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="6.6" fill="#ed2939"/><rect y="6.6" width="30" height="6.8" fill="#fff"/><rect y="13.4" width="30" height="6.6" fill="#ed2939"/></svg>""",
    "อังกฤษ": """<svg class="flag-svg" viewBox="0 0 30 18" width="24" height="16" style="border: 1px solid rgba(255,255,255,0.08)"><rect width="30" height="18" fill="#fff"/><rect y="7.2" width="30" height="3.6" fill="#da291c"/><rect x="13.2" width="3.6" height="18" fill="#da291c"/></svg>""",
    "เกาหลีใต้": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16" style="border: 1px solid rgba(255,255,255,0.08)"><rect width="30" height="20" fill="#fff"/><circle cx="15" cy="10" r="5" fill="#cd2e3a"/><path d="M15 10a5 5 0 0 0 0-10 2.5 2.5 0 0 0 0 5 2.5 2.5 0 0 1 0 5z" fill="#0047a0"/><rect x="4" y="4" width="2" height="4" fill="#000" transform="rotate(30 4 4)"/><rect x="24" y="4" width="2" height="4" fill="#000" transform="rotate(-30 24 4)"/></svg>""",
    "บราซิล": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="20" fill="#009739"/><polygon points="15,2 28,10 15,18 2,10" fill="#fedf00"/><circle cx="15" cy="10" r="5" fill="#2d3092"/><path d="M10 10a12 12 0 0 1 10-2" stroke="#fff" stroke-width="0.8" fill="none"/></svg>""",
    "เซอร์เบีย": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="6.6" fill="#c8102e"/><rect y="6.6" width="30" height="6.8" fill="#0c4076"/><rect y="13.4" width="30" height="6.6" fill="#fff"/><rect x="7" y="5" width="4" height="8" fill="#c8102e" style="opacity:0.8;"/></svg>""",
    "สเปน": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="5" fill="#aa151b"/><rect y="5" width="30" height="10" fill="#f1bf00"/><rect y="15" width="30" height="5" fill="#aa151b"/><circle cx="8" cy="10" r="2" fill="#aa151b"/></svg>""",
    "สวิตเซอร์แลนด์": """<svg class="flag-svg" viewBox="0 0 20 20" width="18" height="18"><rect width="20" height="20" fill="#d81e05"/><rect x="8" y="4" width="4" height="12" fill="#fff"/><rect x="4" y="8" width="12" height="4" fill="#fff"/></svg>""",
    "เยอรมนี": """<svg class="flag-svg" viewBox="0 0 30 18" width="24" height="16"><rect width="30" height="6" fill="#000"/><rect y="6" width="30" height="6" fill="#ff0000"/><rect y="12" width="30" height="6" fill="#ffcc00"/></svg>""",
    "โปรตุเกส": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="12" height="20" fill="#006600"/><rect x="12" width="18" height="20" fill="#ff0000"/><circle cx="12" cy="10" r="3.5" fill="#fedf00"/></svg>""",
    "โครเอเชีย": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="6.6" fill="#ff0000"/><rect y="6.6" width="30" height="6.8" fill="#fff"/><rect y="13.4" width="30" height="6.6" fill="#171796"/><rect x="13" y="5" width="4" height="5" fill="#ff0000" style="opacity:0.8;"/></svg>""",
    "เนเธอร์แลนด์": """<svg class="flag-svg" viewBox="0 0 30 20" width="24" height="16"><rect width="30" height="6.6" fill="#ae1c28"/><rect y="6.6" width="30" height="6.8" fill="#fff"/><rect y="13.4" width="30" height="6.6" fill="#21468b"/></svg>"""
}

def get_flag_svg(country_name):
    if not country_name:
        return '<span style="font-size:1.1rem;">🏳️</span>'
    clean_name = country_name.strip()
    return FLAG_SVGS.get(clean_name, '<span style="font-size:1.1rem;">🏳️</span>')

# --- Preset definitions list in local file scope ---
CHAMPIONS_POOL = [
  { "name": "อาร์เจนตินา", "flag": "🇦🇷" },
  { "name": "ฝรั่งเศส", "flag": "🇫🇷" },
  { "name": "บราซิล", "flag": "🇧🇷" },
  { "name": "อังกฤษ", "flag": "🏴󠁧󠁢󠁥󠁮󠁧󠁿" },
  { "name": "โปรตุเกส", "flag": "🇵🇹" },
  { "name": "เยอรมนี", "flag": "🇩🇪" },
  { "name": "สเปน", "flag": "🇪🇸" },
  { "name": "เนเธอร์แลนด์", "flag": "🇳🇱" },
  { "name": "โครเอเชีย", "flag": "🇭🇷" },
  { "name": "ญี่ปุ่น", "flag": "🇯🇵" }
]

# --- Premium Custom Styling (Stadium Night Theme) ---
st.markdown("""
<style>
    /* Global Background & Fonts */
    .stApp {
        background-color: #0b0f19;
        background-image: 
            radial-gradient(at 0% 0%, rgba(15, 23, 42, 0.9) 0, transparent 50%),
            radial-gradient(at 50% 0%, rgba(30, 41, 59, 0.4) 0, transparent 50%),
            radial-gradient(at 100% 0%, rgba(16, 185, 129, 0.1) 0, transparent 40%);
        color: #f8fafc;
    }
    
    h1, h2, h3, h4, h5, h6, p, label, span {
        font-family: 'Sarabun', 'Inter', sans-serif !important;
    }
    
    /* Card Container styling */
    .panel-card {
        background-color: #1e293b !important;
        border: 1px solid rgba(255, 255, 255, 0.08) !important;
        border-radius: 16px !important;
        padding: 20px !important;
        margin-bottom: 20px !important;
        box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3) !important;
    }
    
    .match-card {
        background-color: rgba(15, 23, 42, 0.5) !important;
        border: 1px solid #334155 !important;
        border-radius: 12px !important;
        padding: 16px !important;
        margin-bottom: 16px !important;
        box-shadow: 0 4px 12px rgba(0,0,0,0.1) !important;
    }
    
    /* Buttons Customization */
    .stButton>button {
        border-radius: 30px !important;
        font-weight: 700 !important;
        font-family: 'Sarabun', 'Inter', sans-serif !important;
        transition: all 0.2s !important;
    }
    
    /* Sidebar styling */
    section[data-testid="stSidebar"] {
        background-color: #0f172a !important;
        border-right: 1px solid rgba(255, 255, 255, 0.08) !important;
    }
    
    /* Custom tags */
    .gold-text {
        color: #fbbf24 !important;
        font-weight: bold;
    }
    .green-text {
        color: #10b981 !important;
        font-weight: bold;
    }
    .red-text {
        color: #ef4444 !important;
        font-weight: bold;
    }

    /* Leaderboard Table styling */
    .leaderboard-table {
        width: 100%;
        border-collapse: collapse;
        margin-top: 10px;
        background-color: #1e293b;
        color: #f8fafc;
    }
    .leaderboard-table th {
        background-color: #0f172a;
        color: #94a3b8;
        font-weight: 700;
        font-size: 0.85rem;
        text-transform: uppercase;
        padding: 12px;
        text-align: left;
        border-bottom: 1px solid #334155;
    }
    .leaderboard-table td {
        padding: 14px 12px;
        border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        vertical-align: middle;
    }
    .leaderboard-row {
        transition: background-color 0.2s;
    }
    .leaderboard-row:hover {
        background-color: rgba(255,255,255,0.02);
    }
    .rank-cell {
        width: 60px;
        text-align: center;
    }
    .player-info-cell {
        display: flex;
        align-items: center;
        gap: 10px;
    }
    .champion-badge {
        font-size: 0.75rem;
        margin-top: 4px;
        color: #94a3b8;
    }
    .correct-champion-text {
        color: #10b981;
        font-weight: 700;
    }
    .points-pill {
        font-weight: 900;
        font-size: 1.15rem;
        padding: 4px 12px;
        border-radius: 20px;
        background: #0f172a;
        display: inline-block;
    }
    .points-pill-gold {
        color: #fbbf24;
        border: 1px solid #fbbf24;
    }
    .flag-svg {
        display: inline-block;
        vertical-align: middle;
        border-radius: 2px;
        box-shadow: 0 1px 3px rgba(0,0,0,0.35);
        margin-right: 6px;
    }
</style>
""", unsafe_allow_html=True)

# --- Session States Initialization ---
if 'selected_user_id' not in st.session_state:
    saved_id = db.get_users()
    st.session_state.selected_user_id = saved_id[0]['user_id'] if saved_id else None

if 'authenticated_user_id' not in st.session_state:
    st.session_state.authenticated_user_id = st.session_state.selected_user_id

if 'pin_prompt_user' not in st.session_state:
    st.session_state.pin_prompt_user = None

if 'pin_input' not in st.session_state:
    st.session_state.pin_input = ''

if 'pin_error' not in st.session_state:
    st.session_state.pin_error = False

if 'auto_simulate' not in st.session_state:
    st.session_state.auto_simulate = True

# Auto-update live scores if enabled (uses actual current system time)
if st.session_state.auto_simulate:
    db.auto_update_live_matches(None)

# Fetch data in real-time
users = db.get_users()
matches = db.get_matches()
predictions = db.get_predictions()

# --- Champion Determination Helper ---
def get_actual_champion():
    final_match = next((m for m in matches if m['match_id'] == 13), None)
    if final_match and final_match['actual_result'] is not None and final_match['actual_result'] != "PENDING":
        if final_match['actual_result'] == "team_a_win":
            return final_match['team_a']
        elif final_match['actual_result'] == "team_b_win":
            return final_match['team_b']
        return "รอดวลจุดโทษ"
    return ""

actual_champion = get_actual_champion()

# --- Leaderboard Calculations ---
def calculate_leaderboard():
    lb = []
    for u in users:
        pts = 0
        correct_wins = 0
        correct_draws = 0
        predicts_count = 0
        
        user_preds = [p for p in predictions if p['user_id'] == u['user_id']]
        
        for pred in user_preds:
            if pred['predicted_result'] is not None:
                predicts_count += 1
                match = next((m for m in matches if m['match_id'] == pred['match_id']), None)
                if match and match['actual_result'] is not None and match['actual_result'] != "PENDING":
                    # Correct Win Predict
                    if match['actual_result'] in ['team_a_win', 'team_b_win'] and pred['predicted_result'] == match['actual_result']:
                        pts += 3
                        correct_wins += 1
                    # Correct Draw Predict
                    elif match['actual_result'] == 'draw' and pred['predicted_result'] == 'draw':
                        pts += 1
                        correct_draws += 1
                        
        # Bonus champion guess (+5 points)
        has_correct_champion = False
        if actual_champion != "" and u['champion_guess'] != "" and u['champion_guess'].strip() == actual_champion.strip():
            pts += 5
            has_correct_champion = True
            
        lb.append({
            'user_id': u['user_id'],
            'display_name': u['display_name'],
            'color_hex': u['color_hex'],
            'champion_guess': u['champion_guess'],
            'points': pts,
            'correct_wins': correct_wins,
            'correct_draws': correct_draws,
            'has_correct_champion': has_correct_champion,
            'total_predictions': predicts_count
        })
    return sorted(lb, key=lambda x: x['points'], reverse=True)

leaderboard = calculate_leaderboard()

# --- Sidebar: User Profiles & Login ---
st.sidebar.markdown(f'<h2 style="color:#fbbf24; text-align:center; font-family: Outfit;">🏆 เซียนบอลโลก 2026</h2>', unsafe_allow_html=True)
st.sidebar.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 10px 0;'>", unsafe_allow_html=True)

# User dropdown switcher
user_options = {u['display_name']: u for u in users}
active_user = next((u for u in users if u['user_id'] == st.session_state.authenticated_user_id), None)

if users:
    st.sidebar.subheader("👤 สลับโปรไฟล์ผู้ใช้งาน")
    
    # Render user selection circles/list
    selected_name = st.sidebar.selectbox(
        "เลือกผู้เล่นเพื่อส่งทำนายผล:",
        options=list(user_options.keys()),
        index=list(user_options.keys()).index(active_user['display_name']) if active_user else 0
    )
    
    target_user = user_options[selected_name]
    
    # Switch profile handler (PIN validation trigger)
    if st.session_state.authenticated_user_id != target_user['user_id']:
        if target_user['pin'] != "":
            st.session_state.pin_prompt_user = target_user
            st.session_state.pin_input = ""
            st.session_state.pin_error = False
        else:
            st.session_state.selected_user_id = target_user['user_id']
            st.session_state.authenticated_user_id = target_user['user_id']
            st.rerun()

# PIN Keyboard Authentication Modal (in Sidebar)
if st.session_state.pin_prompt_user is not None:
    prompt_user = st.session_state.pin_prompt_user
    st.sidebar.warning(f"🔐 โปรไฟล์ '{prompt_user['display_name']}' มีรหัสล็อก PIN")
    
    # Enter PIN dots representation
    dots = " ".join(["●" if len(st.session_state.pin_input) > i else "○" for i in range(4)])
    st.sidebar.markdown(f"<h3 style='text-align:center; color:#fbbf24;'>{dots}</h3>", unsafe_allow_html=True)
    
    if st.session_state.pin_error:
        st.sidebar.error("❌ รหัส PIN ไม่ถูกต้อง")
        
    # Virtual numeric keypad
    cols = st.sidebar.columns(3)
    for i in range(1, 10):
        col_idx = (i - 1) % 3
        with cols[col_idx]:
            if st.button(str(i), key=f"pin_key_{i}", use_container_width=True):
                st.session_state.pin_error = False
                if len(st.session_state.pin_input) < 4:
                    st.session_state.pin_input += str(i)
                    
    with cols[0]:
        if st.button("⌫", key="pin_key_back", use_container_width=True):
            st.session_state.pin_input = st.session_state.pin_input[:-1]
    with cols[1]:
        if st.button("0", key="pin_key_0", use_container_width=True):
            st.session_state.pin_error = False
            if len(st.session_state.pin_input) < 4:
                st.session_state.pin_input += "0"
    with cols[2]:
        if st.button("ยกเลิก", key="pin_key_cancel", use_container_width=True):
            st.session_state.pin_prompt_user = None
            st.session_state.pin_input = ""
            st.session_state.pin_error = False
            st.rerun()
            
    # Automatically verify when 4 digits are input
    if len(st.session_state.pin_input) == 4:
        if st.session_state.pin_input == prompt_user['pin']:
            st.session_state.selected_user_id = prompt_user['user_id']
            st.session_state.authenticated_user_id = prompt_user['user_id']
            st.session_state.pin_prompt_user = None
            st.session_state.pin_input = ""
            st.session_state.pin_error = False
            st.rerun()
        else:
            st.session_state.pin_error = True
            st.session_state.pin_input = ""
            st.rerun()

st.sidebar.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 10px 0;'>", unsafe_allow_html=True)

# Google Login Simulation in sidebar
st.sidebar.subheader("🔑 บัญชี Google จำลอง")
g_cols = st.sidebar.columns(2)
with g_cols[0]:
    if st.button("Pang 🔴", use_container_width=True):
        new_id = db.add_user("Pang", "#EA4335", "", "")
        users = db.get_users()
        target = next((u for u in users if u['display_name'] == "Pang"), None)
        if target:
            st.session_state.selected_user_id = target['user_id']
            st.session_state.authenticated_user_id = target['user_id']
            st.rerun()
with g_cols[1]:
    if st.button("คุณสิริวร 🟢", use_container_width=True):
        new_id = db.add_user("คุณสิริวร", "#34A853", "", "")
        users = db.get_users()
        target = next((u for u in users if u['display_name'] == "คุณสิริวร"), None)
        if target:
            st.session_state.selected_user_id = target['user_id']
            st.session_state.authenticated_user_id = target['user_id']
            st.rerun()

st.sidebar.markdown("<br>", unsafe_allow_html=True)

# Delete active profile trigger
if active_user:
    st.sidebar.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 10px 0;'>", unsafe_allow_html=True)
    if st.sidebar.button("❌ ลบผู้ใช้งานปัจจุบัน", use_container_width=True, type="secondary"):
        if len(users) <= 1:
            st.sidebar.error("ไม่สามารถลบผู้ใช้งานสุดท้ายได้")
        else:
            db.delete_user(active_user['user_id'])
            # Switch to first remaining user
            remaining = db.get_users()
            st.session_state.selected_user_id = remaining[0]['user_id']
            st.session_state.authenticated_user_id = remaining[0]['user_id']
            st.success(f"ลบโปรไฟล์ {active_user['display_name']} เรียบร้อยแล้ว")
            time.sleep(1)
            st.rerun()

# --- Main App Headers ---
st.title("🏆 เซียนบอลโลก 2026")
st.markdown("<p style='color: var(--color-text-gray); margin-top:-10px;'>ทัศนะฟุตบอลโลก สรุปผลคะแนนกลุ่มเพื่อน</p>", unsafe_allow_html=True)

if active_user:
    st.markdown(f"""
    <div style='background-color:rgba(30,41,59,0.3); border:1px solid #334155; border-radius:8px; padding:10px 18px; margin-bottom:24px; display:flex; align-items:center; gap:12px;'>
        <span style='width:12px; height:12px; border-radius:50%; background:{active_user['color_hex']}; display:inline-block;'></span>
        <span style='color:#94a3b8; font-size:0.9rem;'>กำลังกรอกพยากรณ์สำหรับ: </span>
        <strong style='color:#fff; font-size:0.95rem;'>{active_user['display_name']}</strong>
        {f"<span style='background:#ea4335; color:#fff; font-size:0.65rem; padding:2px 6px; border-radius:10px; font-weight:bold;'>Google Account</span>" if active_user['display_name'] in ['Pang', 'คุณสิริวร'] else ''}
    </div>
    """, unsafe_allow_html=True)

# Tabs definitions
tab0, tab1, tab2 = st.tabs(["📊 สรุปผล / ตารางคะแนน", "⚽ ส่งพยากรณ์ผล", "⚙️ ผลบอลสนามจริง (แอดมิน)"])

# --- TAB 0: Leaderboard ---
with tab0:
    st.header("👑 ตารางเทียบคะแนนรวมกลุ่มเพื่อน")
    
    # Leaderboard HTML table presentation
    if not leaderboard:
        st.info("ยังไม่มีข้อมูลผู้เล่น")
    else:
        table_html = """
        <div style="overflow-x: auto;">
            <table class="leaderboard-table">
                <thead>
                    <tr>
                        <th class="rank-cell">อันดับ</th>
                        <th>ผู้เล่น</th>
                        <th style="text-align: center;">สถิติ (ชนะ / เสมอ)</th>
                        <th style="text-align: center;">ส่งทายแล้ว</th>
                        <th style="text-align: center;">คะแนนรวม</th>
                    </tr>
                </thead>
                <tbody>
        """
        
        for idx, row in enumerate(leaderboard):
            rank_str = "👑" if idx == 0 else f"<strong style='color:#94a3b8;'>#{idx+1}</strong>"
            champ_team = row['champion_guess']
            champ_svg = get_flag_svg(champ_team) if champ_team else ""
            
            champ_badge_html = ""
            if champ_team:
                bonus_str = '<span class="correct-champion-text"> (+5 โบนัสแชมป์โลก!)</span>' if row['has_correct_champion'] else ''
                champ_badge_html = f"""
                <div class="champion-badge" style="display:flex; align-items:center; gap:4px; margin-top:4px;">
                    🏆 ทายแชมป์: {champ_svg} {champ_team} {bonus_str}
                </div>
                """
            
            is_gold_pill = "points-pill-gold" if idx == 0 else ""
            user_color = row['color_hex'] if row.get('color_hex') else '#FFC107'
            first_char = row['display_name'][0].upper() if row['display_name'] else '?'
            
            table_html += f"""
                <tr class="leaderboard-row">
                    <td class="rank-cell" style="text-align: center; vertical-align: middle;">{rank_str}</td>
                    <td style="vertical-align: middle;">
                        <div class="player-info-cell">
                            <span style="width: 24px; height: 24px; border-radius: 50%; background: {user_color}; display: inline-flex; align-items: center; justify-content: center; color: #fff; font-weight: 800; font-size: 0.75rem;">
                                {first_char}
                            </span>
                            <div>
                                <strong style="font-size: 0.95rem;">{row['display_name']}</strong>
                                {champ_badge_html}
                            </div>
                        </div>
                    </td>
                    <td style="text-align: center; vertical-align: middle;">
                        🏆 {row['correct_wins']} ครั้ง &nbsp;&nbsp; 🤝 {row['correct_draws']} ครั้ง
                    </td>
                    <td style="text-align: center; vertical-align: middle; color: #94a3b8; font-size: 0.9rem;">
                        {row['total_predictions']} / {len(matches)} คู่
                    </td>
                    <td style="text-align: center; vertical-align: middle;">
                        <span class="points-pill {is_gold_pill}">{row['points']}</span>
                    </td>
                </tr>
            """
            
        table_html += """
                </tbody>
            </table>
        </div>
        """
        st.markdown(table_html, unsafe_allow_html=True)

    # Winner alert banner
    if actual_champion:
        st.success(f"🎉 สิ้นสุดทัวร์นาเมนต์แล้ว! แชมป์โลกฟุตบอลจริงคือ: **{actual_champion}**")
        top_score = leaderboard[0]['points'] if leaderboard else 0
        winners = [l['display_name'] for l in leaderboard if l['points'] == top_score]
        st.balloons()
        champ_svg = get_flag_svg(actual_champion)
        st.markdown(f"""
        <div style='background-color:rgba(16, 185, 129, 0.1); border:2px solid #10b981; border-radius:12px; padding:18px; text-align:center;'>
            <h3 style='color:#10b981; font-weight:800;'>ขอแสดงความยินดีกับผู้ชนะถ้วยรางวัลเวิลด์คัพ!</h3>
            <p style='font-size:1.15rem; color:#fff; margin-top:8px; display:flex; align-items:center; justify-content:center; gap:6px;'>
                ทีมแชมป์โลกจริง: {champ_svg} <strong>{actual_champion}</strong>
            </p>
            <p style='font-size:1.15rem; color:#fff; margin-top:8px;'>ผู้ทำแต้มพยากรณ์สูงสุดคือ: <strong>{", ".join(winners)}</strong> ({top_score} คะแนน)</p>
        </div>
        """, unsafe_allow_html=True)

    # Sidebar style simulation trigger inside Tab
    st.markdown("<br>", unsafe_allow_html=True)
    sim_cols = st.columns(2)
    with sim_cols[0]:
        st.subheader("⚙️ ควบคุมผลการแข่งขันจำลอง")
        st.write("ปุ่มลัดเพื่อสุ่มบันทึกผลการแข่งขันสนามจริงทั้งหมดทันที หรือกดรีเซ็ตผลเพื่อตรวจสอบตารางคะแนนสะสม")
        
        btn_cols = st.columns(2)
        with btn_cols[0]:
            if st.button("🎲 สุ่มผลแข่งจริงจำลองทั้งหมด", use_container_width=True, type="primary"):
                db.simulate_tournament()
                st.success("จำลองผลลัพธ์สนามจริงเรียบร้อยแล้ว!")
                time.sleep(0.5)
                st.rerun()
        with btn_cols[1]:
            if st.button("🔄 รีเซ็ตผลบอลทั้งหมด", use_container_width=True):
                db.reset_all_matches()
                st.success("รีเซ็ตผลการแข่งขันเรียบร้อยแล้ว!")
                time.sleep(0.5)
                st.rerun()
                
    with sim_cols[1]:
        st.subheader("⚖️ กฎระเบียบและกฎหมายเกี่ยวกับการพนัน")
        st.markdown("""
        > [!WARNING]
        > **ข้อควรระวังและพ.ร.บ. การพนัน พ.ศ. 2478:**
        > * การพนันทายผลกีฬาฟุตบอลออนไลน์หรือออฟไลน์เป็นความผิดตามกฎหมายอาญาของประเทศไทย มีโทษทั้งจำคุกและปรับอย่างรุนแรง
        > * แอปพลิเคชันนี้จัดทำขึ้นเพื่อกิจกรรมสร้างสรรค์ กระชับความสัมพันธ์ในครอบครัวหรือกลุ่มเพื่อนสนิท และเพื่อความเพลิดเพลินในการวิเคราะห์เชิงสถิติเท่านั้น **ห้ามนำข้อมูลไปใช้วางเดิมพันแข่งขันพนันแลกเปลี่ยนทรัพย์สินเงินทองอย่างเด็ดขาด**
        """, unsafe_allow_html=True)

# --- TAB 1: Make Predictions ---
with tab1:
    if not active_user:
        st.info("กรุณาสร้างผู้เล่นหรือเลือกโปรไฟล์ในแถบด้านข้างก่อน")
    else:
        st.subheader("🏆 ทำนายผลทีมแชมป์โลก")
        
        # User champion prediction select
        if active_user['champion_guess'] and active_user['champion_guess'].strip() != "":
            champ_team = active_user['champion_guess']
            champ_svg = get_flag_svg(champ_team)
            st.markdown(f"""
            <div style='background:rgba(251,191,36,0.1); border:1px solid #fbbf24; border-radius:8px; padding:10px 14px; margin-bottom:15px; display:flex; align-items:center; gap:8px;'>
                🏆 ทีมแชมป์โลกที่คุณทายไว้: {champ_svg} <strong>{champ_team}</strong> <span style='color:#10b981; font-weight:bold;'> (ยืนยันล็อกแล้ว)</span>
            </div>
            """, unsafe_allow_html=True)
        else:
            selected_champ = st.selectbox(
                "คุณวิเคราะห์ว่าชาติใดจะได้เป็นแชมป์โลก 2026? (ทายถูกรับโบนัส +5 คะแนน):",
                options=["-- เลือกทีมแชมป์ --"] + [c['name'] for c in CHAMPIONS_POOL],
                index=0
            )
            if selected_champ != "-- เลือกทีมแชมป์ --":
                if st.button("💾 ยืนยันการเลือกทีมแชมป์ (ยืนยันแล้วแก้ไขไม่ได้อีก)", key="confirm_champ_btn", type="primary", use_container_width=True):
                    db.update_user_champion(active_user['user_id'], selected_champ)
                    st.success(f"บันทึกทำนายทีมแชมป์: {selected_champ} เรียบร้อยแล้ว (ล็อกการแก้ไข)")
                    time.sleep(0.5)
                    st.rerun()
            
        st.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 20px 0;'>", unsafe_allow_html=True)
        st.subheader("⚽ แมตช์การแข่งขันที่เปิดให้ทายผล")
        
        # Toggle options filter
        only_pending_filter = st.checkbox("แสดงเฉพาะคู่ที่ยังไม่แข่งขัน (Pending)", value=True)
        
        # Current local date-time check
        now = datetime.now()
        
        for match in matches:
            # Filters check
            if only_pending_filter and match['actual_result'] is not None and match['actual_result'] != "PENDING":
                continue
                
            match_id = match['match_id']
            kickoff_dt = datetime.fromisoformat(match['kickoff_time'])
            
            # Lock validations
            is_past_kickoff = now > kickoff_dt
            is_locked = is_past_kickoff or match['locked'] == 1 or (match['actual_result'] is not None and match['actual_result'] != "PENDING")
            
            # Fetch active user prediction
            user_pred = next((p for p in predictions if p['user_id'] == active_user['user_id'] and p['match_id'] == match_id), None)
            is_submitted = (user_pred is not None) and (user_pred['predicted_result'] is not None) and (user_pred['predicted_result'] != "PENDING")
            
            if is_submitted:
                current_choice = user_pred['predicted_result']
            else:
                if 'temp_predictions' not in st.session_state:
                    st.session_state.temp_predictions = {}
                current_choice = st.session_state.temp_predictions.get(match_id, "PENDING")

            # Display match card HTML layout
            cols = st.columns([1.5, 3, 1.5])
            
            with cols[0]:
                st.markdown(f"<span style='font-size:0.75rem; background:#0f172a; padding:3px 8px; border-radius:10px; color:#fbbf24; font-weight:700;'>{match['stage']}</span>", unsafe_allow_html=True)
                st.write(f"⏰ {kickoff_dt.strftime('%d/%m/%Y %H:%M น.')}")
                
            with cols[1]:
                # VS matchups description
                score_display = "VS"
                if match['actual_result'] is not None and match['actual_result'] != "PENDING":
                    score_display = f"{match['team_a_score']} - {match['team_b_score']}"
                    
                team_a_svg = get_flag_svg(match['team_a'])
                team_b_svg = get_flag_svg(match['team_b'])
                st.markdown(f"""
                <div style='display:flex; align-items:center; justify-content:center; gap:14px; margin-top:5px;'>
                    <span style='display:inline-flex; align-items:center; gap:6px; font-weight:700;'>{match['team_a']} {team_a_svg}</span>
                    <span style='background:#0f172a; border:1px solid #334155; padding:4px 10px; border-radius:6px; font-weight:bold; color:#fbbf24;'>{score_display}</span>
                    <span style='display:inline-flex; align-items:center; gap:6px; font-weight:700;'>{team_b_svg} {match['team_b']}</span>
                </div>
                """, unsafe_allow_html=True)
                
            with cols[2]:
                if match['status'] == 'live':
                    ref_dt = datetime.now()
                    delta = ref_dt - kickoff_dt
                    elapsed_min = int(delta.total_seconds() / 60)
                    if elapsed_min < 45:
                        minute_label = f"{elapsed_min}'"
                    elif elapsed_min < 50:
                        minute_label = "HT"
                    elif elapsed_min < 95:
                        minute_label = f"{elapsed_min - 5}'"
                    else:
                        minute_label = "FT"
                    st.markdown(f"<span style='background:rgba(16,185,129,0.15); color:#10b981; padding:4px 10px; border-radius:20px; font-size:0.75rem; font-weight:900; box-shadow:0 0 10px rgba(16,185,129,0.3);'>🟢 LIVE {minute_label}</span>", unsafe_allow_html=True)
                elif is_locked:
                    st.markdown("<span style='color:#ef4444; font-size:0.8rem; font-weight:700;'>🔒 ปิดรับการทายผล</span>", unsafe_allow_html=True)
                elif is_submitted:
                    st.markdown("<span style='color:#10b981; font-size:0.8rem; font-weight:700;'>🔒 ทายสำเร็จแล้ว</span>", unsafe_allow_html=True)
                else:
                    st.markdown("<span style='color:#10b981; font-size:0.8rem; font-weight:700;'>🔓 เปิดรับทาย</span>", unsafe_allow_html=True)
            
            # Prediction inputs choices row
            choice_cols = st.columns(3)
            options = [
                ("team_a_win", f"{match['team_a_flag']} {match['team_a']} ชนะ"),
                ("draw", "🤝 เสมอ"),
                ("team_b_win", f"{match['team_b_flag']} {match['team_b']} ชนะ")
            ]
            
            is_button_disabled = is_locked or is_submitted
            
            for o_idx, (opt_val, opt_label) in enumerate(options):
                with choice_cols[o_idx]:
                    btn_type = "primary" if current_choice == opt_val else "secondary"
                    # Handle disabled if locked or already submitted
                    if st.button(
                        opt_label, 
                        key=f"pred_btn_{match_id}_{opt_val}", 
                        disabled=is_button_disabled,
                        type=btn_type,
                        use_container_width=True
                    ):
                        st.session_state.temp_predictions[match_id] = opt_val
                        st.rerun()
                        
            # Show Confirm Button if temp prediction exists and not submitted
            if not is_submitted and current_choice != "PENDING":
                if st.button(
                    "💾 ยืนยันผลทายคู่นี้ (กดยืนยันแล้วแก้ไขไม่ได้อีก)", 
                    key=f"confirm_btn_{match_id}", 
                    type="primary", 
                    use_container_width=True
                ):
                    db.submit_prediction(active_user['user_id'], match_id, current_choice)
                    if match_id in st.session_state.temp_predictions:
                        del st.session_state.temp_predictions[match_id]
                    st.success("บันทึกคำทำนายและล็อกเรียบร้อยแล้ว!")
                    time.sleep(0.3)
                    st.rerun()
            elif is_submitted:
                st.markdown("<div style='text-align:center; color:#10b981; font-weight:bold; font-size:0.85rem; margin-top:5px;'>🔒 ยืนยันผลทายเรียบร้อยแล้ว (แก้ไขไม่ได้)</div>", unsafe_allow_html=True)
                        
            # Feedback badge display
            if match['actual_result'] is not None and match['actual_result'] != "PENDING":
                is_correct = False
                if match['actual_result'] in ['team_a_win', 'team_b_win'] and current_choice == match['actual_result']:
                    is_correct = True
                elif match['actual_result'] == 'draw' and current_choice == 'draw':
                    is_correct = True
                    
                reward = 1 if match['actual_result'] == 'draw' else 3
                
                if is_correct:
                    st.markdown(f"<span style='color:#10b981; font-size:0.8rem; font-weight:bold;'>✓ ทายถูกต้อง (+{reward} คะแนน)</span>", unsafe_allow_html=True)
                else:
                    st.markdown("<span style='color:#ef4444; font-size:0.8rem; font-weight:bold;'>✗ ทายผิด (+0 คะแนน)</span>", unsafe_allow_html=True)
                    
            st.markdown("<hr style='border-color: rgba(255,255,255,0.05); margin: 12px 0;'>", unsafe_allow_html=True)

# --- TAB 2: Admin Panel ---
with tab2:
    st.subheader("📣 แผงควบคุมแอดมิน / คณะกรรมการ")
    st.write("ส่วนจัดบันทึกผลคะแนนแข่งขันจริงและควบคุมสถานะการทายผลบอล")
    
    # Live score simulator & online sync control panel
    st.markdown("<div style='background-color:rgba(30,41,59,0.4); border:1px solid rgba(255,255,255,0.08); border-radius:12px; padding:16px; margin-bottom:20px;'>", unsafe_allow_html=True)
    st.write("⚙️ **ระบบจำลองคะแนนสดตามเวลาจริง & ซิงค์อินเตอร์เน็ต**")
    
    col1, col2 = st.columns(2)
    with col1:
        auto_sim = st.toggle("เปิดระบบจำลองคะแนนสดตามเวลาจริง", value=st.session_state.auto_simulate)
        if auto_sim != st.session_state.auto_simulate:
            st.session_state.auto_simulate = auto_sim
            st.rerun()
            
        st.markdown("<br>", unsafe_allow_html=True)
        st.write("🌐 **ดึงผลจาก Football-Data.org API จริง**")
        default_token = os.environ.get("FOOTBALL_DATA_API_TOKEN", "")
        if 'api_token' not in st.session_state:
            st.session_state.api_token = default_token
            
        api_token_input = st.text_input("ป้อน Football-Data API Token ของคุณ:", value=st.session_state.api_token, type="password", help="ลงทะเบียนรับฟรีได้ที่ football-data.org")
        if api_token_input != st.session_state.api_token:
            st.session_state.api_token = api_token_input
            
        if st.button("🌐 ซิงค์ผลบอลสดจาก API (Live Sync)", use_container_width=True, type="primary"):
            import sync_api
            success, msg = sync_api.run_sync(api_token_input)
            if success:
                st.success(f"🎉 {msg}")
                time.sleep(1)
                st.rerun()
            else:
                st.error(f"❌ {msg}")
                
    with col2:
        st.write("🌐 **ซิงค์ผลบอลจากไฟล์ JSON (แมนวล)**")
        sync_url = st.text_input("ป้อน URL หรือชื่อไฟล์ JSON ผลการแข่งขัน:", value="live_scores.json")
        if st.button("📥 เริ่มต้นการซิงค์ข้อมูลผลบอล (Sync Now)", use_container_width=True):
            try:
                import json
                if sync_url.startswith("http://") or sync_url.startswith("https://"):
                    import urllib.request
                    req = urllib.request.Request(sync_url, headers={'User-Agent': 'Mozilla/5.0'})
                    with urllib.request.urlopen(req) as response:
                        data = json.loads(response.read().decode())
                else:
                    import os
                    filepath = sync_url
                    if not os.path.exists(filepath) and os.path.exists(os.path.join("..", filepath)):
                        filepath = os.path.join("..", filepath)
                    with open(filepath, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                
                if 'matches' in data:
                    db.sync_scores_from_json(data['matches'])
                    st.success("🎉 ซิงค์ผลการแข่งขันและคำนวณคะแนนเรียบร้อยแล้ว!")
                    time.sleep(1)
                    st.rerun()
                else:
                    st.error("รูปแบบไฟล์ JSON ไม่ถูกต้อง (ไม่พบคีย์ 'matches')")
            except Exception as e:
                st.error(f"เกิดข้อผิดพลาดในการดึงข้อมูล: {str(e)}")
                
    st.markdown("</div>", unsafe_allow_html=True)
    
    with st.expander("🕒 วิธีตั้งค่าระบบอัปเดตผลบอลอัตโนมัติ (Cron Job / GitHub Actions)"):
        st.markdown("""
        แอปพลิเคชันนี้รองรับการอัปเดตข้อมูลอัตโนมัติโดยใช้ **GitHub Actions** เพื่อคอยวิ่งไปถาม API ทุกๆ 10-15 นาที แล้วบันทึกลงฐานข้อมูลและสร้างไฟล์ `live_scores.json` กลับขึ้นมาบน Git
        
        **วิธีใช้งานบน GitHub Actions:**
        1. สมัครขอรับ API Token ฟรีได้จาก [football-data.org](https://www.football-data.org/)
        2. นำรหัส API Token ที่ได้ไปตั้งใน Action Secrets ของ Repository ของคุณ:
           - ไปที่หน้า GitHub Repository -> **Settings** -> **Secrets and variables** -> **Actions** -> **New repository secret**
           - **Name:** `FOOTBALL_DATA_API_TOKEN`
           - **Value:** *ป้อน Token ที่ได้*
        3. workflow จะรันสคริปต์ `streamlit_app/sync_api.py` ทุกๆ 10-15 นาทีโดยอัตโนมัติ ทำให้ผลบอลสดและอันดับของเพื่อนๆ อัปเดตอยู่เสมอ!
        """)

    st.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 20px 0;'>", unsafe_allow_html=True)
    
    for match in matches:
        match_id = match['match_id']
        st.markdown(f"**{match['stage']} : {match['team_a']} vs {match['team_b']}**")
        
        # Edit kickoff and locks inline
        setup_cols = st.columns([2, 1, 1])
        with setup_cols[0]:
            # Convert string kickoff to datetime for picker
            curr_kickoff = datetime.fromisoformat(match['kickoff_time'])
            new_kickoff = st.date_input(
                f"วันที่คิกออฟ {match['team_a']} vs {match['team_b']}", 
                value=curr_kickoff.date(),
                key=f"adm_date_{match_id}"
            )
            new_time = st.time_input(
                f"เวลาคิกออฟ", 
                value=curr_kickoff.time(),
                key=f"adm_time_{match_id}"
            )
            comb_dt = datetime.combine(new_kickoff, new_time)
            
            if comb_dt.isoformat() != match['kickoff_time']:
                db.update_match_kickoff(match_id, comb_dt.isoformat())
                st.success("อัปเดตเวลาเตะเรียบร้อย")
                st.rerun()
                
        with setup_cols[1]:
            st.write("สถานะการล็อก")
            lock_label = "🔒 ล็อกแล้ว (เปิด)" if match['locked'] == 1 else "🔓 เปิดรับทาย (ปิด)"
            if st.button(lock_label, key=f"adm_lock_{match_id}", use_container_width=True):
                db.toggle_match_lock(match_id)
                st.rerun()
                
        with setup_cols[2]:
            st.write("ล้างบันทึกผล")
            if st.button("ล้างผลบอล", key=f"adm_clear_{match_id}", use_container_width=True):
                db.clear_match_result(match_id)
                st.rerun()

        # Score adjustment input areas
        score_cols = st.columns([1.5, 1, 0.5, 1, 1.5])
        score_a = match['team_a_score'] if match['team_a_score'] >= 0 else 0
        score_b = match['team_b_score'] if match['team_b_score'] >= 0 else 0
        
        with score_cols[0]:
            team_a_svg = get_flag_svg(match['team_a'])
            st.markdown(f"<div style='text-align:right; font-weight:700; display:flex; align-items:center; justify-content:flex-end; gap:6px;'>{match['team_a']} {team_a_svg}</div>", unsafe_allow_html=True)
        with score_cols[1]:
            new_score_a = st.number_input("สกอร์", min_value=0, max_value=20, value=score_a, key=f"adm_scA_{match_id}", label_visibility="collapsed")
        with score_cols[2]:
            st.markdown(f"<div style='text-align:center; font-weight:800; font-size:1.1rem; color:#fbbf24;'>:</div>", unsafe_allow_html=True)
        with score_cols[3]:
            new_score_b = st.number_input("สกอร์", min_value=0, max_value=20, value=score_b, key=f"adm_scB_{match_id}", label_visibility="collapsed")
        with score_cols[4]:
            team_b_svg = get_flag_svg(match['team_b'])
            st.markdown(f"<div style='text-align:left; font-weight:700; display:flex; align-items:center; justify-content:flex-start; gap:6px;'>{team_b_svg} {match['team_b']}</div>", unsafe_allow_html=True)
            
        # Trigger save score if changed
        if new_score_a != match['team_a_score'] or new_score_b != match['team_b_score']:
            res = "draw"
            if new_score_a > new_score_b:
                res = "team_a_win"
            elif new_score_a < new_score_b:
                res = "team_b_win"
            db.update_match_result(match_id, new_score_a, new_score_b, res)
            st.success("บันทึกสกอร์การแข่งขัน")
            time.sleep(0.3)
            st.rerun()

        # Shortcut outcomes buttons
        override_cols = st.columns(3)
        with override_cols[0]:
            if st.button(f"บันทึก {match['team_a']} ชนะ (2-0)", key=f"adm_over_a_{match_id}", use_container_width=True):
                db.update_match_result(match_id, 2, 0, "team_a_win")
                st.rerun()
        with override_cols[1]:
            if st.button(f"บันทึก เสมอ (1-1)", key=f"adm_over_d_{match_id}", use_container_width=True):
                db.update_match_result(match_id, 1, 1, "draw")
                st.rerun()
        with override_cols[2]:
            if st.button(f"บันทึก {match['team_b']} ชนะ (0-2)", key=f"adm_over_b_{match_id}", use_container_width=True):
                db.update_match_result(match_id, 0, 2, "team_b_win")
                st.rerun()
                
        st.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 24px 0;'>", unsafe_allow_html=True)

# --- Sidebar form: Add profile profile ---
st.sidebar.markdown("<hr style='border-color: rgba(255,255,255,0.08); margin: 10px 0;'>", unsafe_allow_html=True)
st.sidebar.subheader("➕ สมัครสมาชิกกลุ่มใหม่")
with st.sidebar.form("add_user_form", clear_on_submit=True):
    new_name = st.text_input("ชื่อผู้ใช้งาน (เพื่อนๆ โหวต):")
    new_pin = st.text_input("ตั้งรหัส PIN 4 หลัก (เว้นว่างได้):", max_chars=4, type="password")
    # Clean non-digits
    new_pin = "".join(filter(str.isdigit, new_pin))
    
    new_color = st.color_picker("เลือกสีโปรไฟล์:", value="#FFC107")
    
    new_champ = st.selectbox(
        "ทายผลทีมแชมป์โลก (โบนัส +5 คะแนน):",
        options=["-- ทายแชมป์ภายหลัง --"] + [c['name'] for c in CHAMPIONS_POOL]
    )
    
    submitted = st.form_submit_form = st.form_submit_button("สร้างโปรไฟล์ผู้เล่น", use_container_width=True)
    if submitted:
        if new_name.strip() == "":
            st.error("กรุณากรอกชื่อผู้เล่น")
        else:
            champ_value = "" if new_champ == "-- ทายแชมป์ภายหลัง --" else new_champ
            new_id = db.add_user(new_name.strip(), new_color, new_pin, champ_value)
            if new_id:
                st.session_state.selected_user_id = new_id
                st.session_state.authenticated_user_id = new_id
                st.success(f"สร้างโปรไฟล์ {new_name} เรียบร้อยแล้ว")
                time.sleep(0.5)
                st.rerun()
            else:
                st.error("ชื่อนี้ถูกใช้งานแล้วในระบบ")


