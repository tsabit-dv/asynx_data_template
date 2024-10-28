const localstorage = '';
const supabaseUrl = ''; 
const supabaseAnonKey = '';

const supabase = createClient(supabaseUrl, supabaseAnonKey);

function saveToLocalStorage(key, data) {
  localStorage.setItem(key, JSON.stringify(data));
}

function getFromLocalStorage(key) {
  const data = localStorage.getItem(key);
  return data ? JSON.parse(data) : null;
}

async function synchronizeData(key, data) {
    const { data: existingData, error } = await supabase
    .from('table_name') 
    .select('*')
    .eq('key', key);

  if (error) {
    console.error('Error fetch data from Supabase:', error);
    return;
  }

  if (existingData.length > 0) {
    const { error: updateError } = await supabase
      .from('table_name')
      .update({ data: JSON.stringify(data) })
      .eq('key', key);

    if (updateError) {
      console.error('Error updating data in Supabase:', updateError);
    }
  } else {
    const { error: insertError } = await supabase
      .from('table_name')
      .insert({ key: key, data: JSON.stringify(data) });

    if (insertError) {
      console.error('Error inserting data into Supabase:', insertError);
    }
  }
}

const myData = { name: 'John Doe', age: 30 };
saveToLocalStorage('user_data', myData);
synchronizeData('user_data', myData);

const storedData = getFromLocalStorage('user_data');
console.log('Data from local storage:', storedData);
