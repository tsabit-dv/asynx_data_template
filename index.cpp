#include <iostream>
#include <string>
#include <fstream>
#include <filesystem>
#include <thread>
#include <future>
#include <chrono>
#include <nlohmann/json.hpp>
#include <supabase/supabase.hpp> 
#include <your_async_library.hpp>

using namespace std;
using namespace supabase;
using json = nlohmann::json;

const string supabaseUrl = ""; 
const string supabaseAnonKey = "";

void saveToLocalFile(const string& key, const json& data) {
    ofstream file("local_data.json");
    file << data.dump();
}

json getFromLocalFile(const string& key) {
    ifstream file("local_data.json");
    json data;
    file >> data;
    return data;
}

auto synchronizeData(const string& key, const json& data) {
    return your_async_library::async([key, data]() {
        Client client(supabaseUrl, supabaseAnonKey);

        auto response = client.from("t_N").select("*").eq("key", key).execute();
        if (response.error) {
            cerr << "Error fetch data from Supabase: " << response.error->message << endl;
            return;
        }
        if (!response.data.empty()) {
            auto updateResponse = client.from("t_N").update({{"data", data.dump()}}).eq("key", key).execute();
            if (updateResponse.error) {
                cerr << "Error updating data in Supabase: " << updateResponse.error->message << endl;
            }
        } else {
            auto insertResponse = client.from("t_N").insert({{"key", key}, {"data", data.dump()}}).execute();
            if (insertResponse.error) {
                cerr << "Error inserting data into Supabase: " << insertResponse.error->message << endl;
            }
        }
    });
}

int main() {
    json myData = {{"name", "John Doe"}, {"age", 30}};
    saveToLocalFile("user_data", myData);

    auto future = synchronizeData("user_data", myData);
    future.wait(); 

    json storedData = getFromLocalFile("user_data");
    cout << "Data from local file: " << storedData.dump() << endl;

    return 0;
}