This project was created to fulfill the mid-semester exam grade for the Platform-Based Programming course.

Members : 
- Muhammad Ath Thaariq - 2210511090
- Rafli Fadlurrohman - 2210511104
- Muhammad Fadawkas Oemarki - 2210511118
- Ridhan Fadhlil Wafi - 2210511118
- Abdef Rasendriya Indrastata - 2210511120
- Hasbul Ihza Firnanda - 2210511124

To Run Backend Services (2024-2025 data)
Run on Android Studio/Compiler Terminal

cd liga1_api
python -m venv .venv
.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
pip install tzdata
uvicorn app.main:app --host 0.0.0.0 --port 1118 --reload
