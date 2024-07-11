// example.ts
import { createContext, useContext, useEffect } from 'react';
import * as R from 'ramda';
import fs from 'fs';

interface User {
  name: string;
  age: number;
}

const UserContext = createContext<User | null>(null);

const App: React.FC = () => {
  const user = useContext(UserContext);

  useEffect(() => {
    console.log(`User name is ${user?.name}`);
  }, [user]);

  return <div>Hello, {user?.name}</div>;
};

export default App;