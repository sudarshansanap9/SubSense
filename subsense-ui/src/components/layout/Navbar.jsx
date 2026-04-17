import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { LogOut, Activity, PlusCircle } from 'lucide-react';

const Navbar = () => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <nav className="glass-panel sticky top-0 z-50 border-b border-white/[0.04] rounded-none shadow-none bg-surface/40">
      <div className="container mx-auto px-6 py-5 flex justify-between items-center">
        <Link to="/" className="text-xl font-bold text-foreground flex items-center gap-3 tracking-tight group">
          <div className="p-2 bg-primary/10 rounded-xl group-hover:bg-primary/20 transition-colors">
            <Activity className="text-primary w-5 h-5" />
          </div>
          <span>SubSense</span>
        </Link>
        
        <div className="flex items-center gap-6">
          <Link to="/add-transaction" className="text-muted hover:text-foreground flex items-center gap-2 transition-colors font-medium text-sm">
            <PlusCircle size={16} className="text-primary/70" />
            <span className="hidden sm:inline">Add Transaction</span>
          </Link>
          
          <div className="h-5 w-px bg-white/10"></div>
          
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3">
              <div className="h-8 w-8 rounded-full bg-secondary/20 flex items-center justify-center text-sm font-bold text-secondary border border-secondary/30">
                {user?.name ? user.name.charAt(0).toUpperCase() : 'U'}
              </div>
              <span className="text-sm font-medium text-foreground">{user?.name || 'User'}</span>
            </div>
            <button 
              onClick={handleLogout}
              className="p-2 rounded-xl text-muted hover:text-danger hover:bg-danger/10 transition-all"
              title="Logout"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
