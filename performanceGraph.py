import os
import glob
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from datetime import datetime

sns.set_theme(style="whitegrid")
plt.rcParams.update({'font.size': 12})

def load_performance_data(log_dir="performance_logs"):
    all_data = []

    csv_files = [f for f in glob.glob(f"{log_dir}/*.csv") if not f.endswith("_distribution.csv")]
    
    if not csv_files:
        print(f"No CSV files found in {log_dir}! Please check the directory path.")
        return None
    
    print(f"Found {len(csv_files)} CSV files to process:")
    for file_path in csv_files:
        print(f"  - {os.path.basename(file_path)}")
        
        try:
            df = pd.read_csv(file_path)
            
            print(f"Successfully loaded file. First few rows:")
            print(df.head(2).to_string())
            print(f"Columns: {df.columns.tolist()}")
            
            filename = os.path.basename(file_path)
            test_type = "regular" if "regular" in filename else "virtual-thread"
            
            if 'test_type' not in df.columns:
                df['test_type'] = test_type
                
            df['file'] = filename
            
            all_data.append(df)
        except Exception as e:
            print(f"Error loading {file_path}: {e}")
    
    if all_data:
        combined_data = pd.concat(all_data, ignore_index=True)
        print(f"Combined data shape: {combined_data.shape}")
        
        required_columns = ['concurrent_users', 'test_type', 'requests_per_second', 'avg_response_time']
        missing_columns = [col for col in required_columns if col not in combined_data.columns]
        
        if missing_columns:
            print(f"Warning: Missing required columns: {missing_columns}")
            
            if 'concurrent_users' not in combined_data.columns and 'concurrentUsers' in combined_data.columns:
                combined_data['concurrent_users'] = combined_data['concurrentUsers']
                print("Fixed: Mapped 'concurrentUsers' to 'concurrent_users'")
            
            if 'avg_response_time' not in combined_data.columns and 'avgResponseTimeMs' in combined_data.columns:
                combined_data['avg_response_time'] = combined_data['avgResponseTimeMs']
                print("Fixed: Mapped 'avgResponseTimeMs' to 'avg_response_time'")
        
        return combined_data
    else:
        print("No data could be loaded!")
        return None

def load_distribution_data(log_dir="performance_logs"):
    """Load response time distribution data with better error handling."""
    all_dists = []
    
    dist_files = glob.glob(f"{log_dir}/*_distribution.csv")
    
    if not dist_files:
        print(f"No distribution CSV files found in {log_dir}!")
        return None
    
    print(f"Found {len(dist_files)} distribution files to process:")
    
    for file_path in dist_files:
        print(f"  - {os.path.basename(file_path)}")
        
        try:
            filename = os.path.basename(file_path).replace("_distribution.csv", "")
            test_type = "regular" if "regular" in filename else "virtual-thread"
            
            df = pd.read_csv(file_path)
            print(f"Distribution columns: {df.columns.tolist()}")
            
            if 'response_time_bucket' not in df.columns or 'count' not in df.columns:
                print(f"Warning: Missing required columns in {filename}")
                print(f"Available columns: {df.columns.tolist()}")
                continue
            
            df['test_type'] = test_type
            df['file'] = filename
            
            all_dists.append(df)
        except Exception as e:
            print(f"Error loading distribution {file_path}: {e}")
    
    if all_dists:
        combined_data = pd.concat(all_dists, ignore_index=True)
        print(f"Combined distribution data shape: {combined_data.shape}")
        return combined_data
    else:
        print("No distribution data could be loaded!")
        return None

def create_throughput_comparison(data):
    """Create a comparison of throughput (requests per second) by concurrent users."""
    if data is None:
        return
    
    try:
        if 'concurrent_users' not in data.columns or 'test_type' not in data.columns or 'requests_per_second' not in data.columns:
            print("Error: Missing required columns for throughput comparison")
            print(f"Available columns: {data.columns.tolist()}")
            return
        
        grouped = data.groupby(['concurrent_users', 'test_type'])['requests_per_second'].mean().reset_index()
        
        print("Grouped throughput data:")
        print(grouped.to_string())
        
        pivot_data = grouped.pivot(index='concurrent_users', columns='test_type', values='requests_per_second')
        
        plt.figure(figsize=(12, 8))
        
        ax = pivot_data.plot(kind='line', marker='o', linewidth=2.5)
        
        plt.xlabel("Concurrent Users")
        plt.ylabel("Requests per Second")
        plt.title("Login Throughput Comparison: Virtual Threads vs. Regular Threads")
        
        if 'regular' in pivot_data.columns and 'virtual-thread' in pivot_data.columns:
            improvement = (pivot_data['virtual-thread'] / pivot_data['regular']).fillna(0)
            for idx, row in pivot_data.iterrows():
                if not np.isnan(row['regular']) and not np.isnan(row['virtual-thread']) and row['regular'] > 0:
                    plt.annotate(f"{improvement[idx]:.1f}x", 
                                xy=(idx, row['virtual-thread']),
                                xytext=(0, 10),
                                textcoords='offset points',
                                ha='center')
        
        plt.grid(True)
        plt.legend(title="Thread Type")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        plt.savefig(f"login_throughput_comparison_{timestamp}.png", dpi=300, bbox_inches='tight')
        print(f"Saved throughput comparison: login_throughput_comparison_{timestamp}.png")
        
        plt.show()
    except Exception as e:
        print(f"Error creating throughput comparison: {e}")

def create_response_time_comparison(data):
    """Create a comparison of response times by concurrent users."""
    if data is None:
        return
    
    try:
        if 'concurrent_users' not in data.columns or 'test_type' not in data.columns:
            print("Error: Missing required columns for response time comparison")
            print(f"Available columns: {data.columns.tolist()}")
            return
        
        response_time_col = None
        for col in ['avg_response_time', 'avgResponseTimeMs', 'avgResponseTime']:
            if col in data.columns:
                response_time_col = col
                break
        
        if response_time_col is None:
            print("Error: Could not find response time column")
            return
            
        grouped = data.groupby(['concurrent_users', 'test_type'])[response_time_col].mean().reset_index()
        
        print("Grouped response time data:")
        print(grouped.to_string())
        
        pivot_data = grouped.pivot(index='concurrent_users', columns='test_type', values=response_time_col)
        
        plt.figure(figsize=(12, 8))
        
        ax = pivot_data.plot(kind='line', marker='o', linewidth=2.5)
        
        plt.xlabel("Concurrent Users")
        plt.ylabel("Average Response Time (ms)")
        plt.title("Login Response Time Comparison: Virtual Threads vs. Regular Threads")
        
        if 'regular' in pivot_data.columns and 'virtual-thread' in pivot_data.columns:
            improvement = (pivot_data['regular'] / pivot_data['virtual-thread']).fillna(0)
            for idx, row in pivot_data.iterrows():
                if not np.isnan(row['regular']) and not np.isnan(row['virtual-thread']) and row['virtual-thread'] > 0:
                    plt.annotate(f"{improvement[idx]:.1f}x faster", 
                                xy=(idx, row['virtual-thread']),
                                xytext=(0, 10),
                                textcoords='offset points',
                                ha='center')
        
        plt.grid(True)
        plt.legend(title="Thread Type")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        plt.savefig(f"login_response_time_comparison_{timestamp}.png", dpi=300, bbox_inches='tight')
        print(f"Saved response time comparison: login_response_time_comparison_{timestamp}.png")
        
        plt.show()
    except Exception as e:
        print(f"Error creating response time comparison: {e}")

def create_distribution_comparison(dist_data):
    """Create a response time distribution comparison."""
    if dist_data is None:
        return
    
    try:
        if 'response_time_bucket' not in dist_data.columns or 'count' not in dist_data.columns or 'test_type' not in dist_data.columns:
            print("Error: Missing required columns for distribution comparison")
            print(f"Available columns: {dist_data.columns.tolist()}")
            return
            
        plt.figure(figsize=(14, 8))
        
        dist_data['response_time_bucket'] = pd.to_numeric(dist_data['response_time_bucket'], errors='coerce')
        
        dist_data = dist_data.dropna(subset=['response_time_bucket'])
        
        grouped = dist_data.groupby(['test_type', 'response_time_bucket'])['count'].sum().reset_index()
        
        print("Grouped distribution data (first few rows):")
        print(grouped.head(10).to_string())
        
        for test_type, group in grouped.groupby('test_type'):
            bucket_max = 3000
            subset = group[group['response_time_bucket'] <= bucket_max]
            
            plt.bar(subset['response_time_bucket'], subset['count'], 
                    alpha=0.5, label=test_type, width=75)
        
        plt.xlabel("Response Time (ms)")
        plt.ylabel("Number of Requests")
        plt.title("Response Time Distribution: Virtual Threads vs. Regular Threads")
        plt.legend(title="Thread Type")
        plt.grid(axis='y', linestyle='--', alpha=0.7)
        
        plt.xticks(np.arange(0, 3001, 300))
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        plt.savefig(f"login_response_time_distribution_{timestamp}.png", dpi=300, bbox_inches='tight')
        print(f"Saved distribution comparison: login_response_time_distribution_{timestamp}.png")
        
        plt.show()
    except Exception as e:
        print(f"Error creating distribution comparison: {e}")

def create_success_rate_comparison(data):
    """Create a comparison of success rates by concurrent users."""
    if data is None:
        return
    
    try:
        required_cols = ['concurrent_users', 'test_type']
        success_cols = ['successes', 'requests']
        
        for col in required_cols:
            if col not in data.columns:
                print(f"Error: Missing required column '{col}' for success rate comparison")
                print(f"Available columns: {data.columns.tolist()}")
                return
        
        has_success_data = all(col in data.columns for col in success_cols)
        
        if has_success_data:
            data['success_rate'] = (data['successes'] / data['requests']) * 100
        else:
            if 'success_rate' not in data.columns:
                print("Warning: Cannot calculate success rate, no success data available")
                return
        
        grouped = data.groupby(['concurrent_users', 'test_type'])['success_rate'].mean().reset_index()
        
        print("Grouped success rate data:")
        print(grouped.to_string())
        
        pivot_data = grouped.pivot(index='concurrent_users', columns='test_type', values='success_rate')
        
        plt.figure(figsize=(12, 8))
        
        ax = pivot_data.plot(kind='line', marker='o', linewidth=2.5)
        
        plt.xlabel("Concurrent Users")
        plt.ylabel("Success Rate (%)")
        plt.title("Login Success Rate Comparison: Virtual Threads vs. Regular Threads")
        
        plt.ylim(0, 105)
        
        plt.grid(True)
        plt.legend(title="Thread Type")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        plt.savefig(f"login_success_rate_comparison_{timestamp}.png", dpi=300, bbox_inches='tight')
        print(f"Saved success rate comparison: login_success_rate_comparison_{timestamp}.png")
        
        plt.show()
    except Exception as e:
        print(f"Error creating success rate comparison: {e}")

def main():
    print("=== Java Login Performance Visualization Tool ===")
    
    log_dir = "performance_logs"
    if not os.path.exists(log_dir):
        print(f"Warning: Directory '{log_dir}' not found!")
        print("Current directory:", os.getcwd())
        print("Contents:", os.listdir("."))
        
        for root, dirs, files in os.walk("."):
            csv_files = [f for f in files if f.endswith(".csv")]
            if csv_files:
                log_dir = root
                print(f"Found CSV files in: {log_dir}")
                break
    
    print("\nLoading performance data...")
    data = load_performance_data(log_dir)
    
    print("\nLoading distribution data...")
    dist_data = load_distribution_data(log_dir)
    
    if data is not None:
        print("\nCreating visualizations...")
        create_throughput_comparison(data)
        create_response_time_comparison(data)
        create_success_rate_comparison(data)
    else:
        print("\nError: No performance data available for visualization!")
    
    if dist_data is not None:
        create_distribution_comparison(dist_data)
    else:
        print("\nNo distribution data available for visualization.")
    
    print("\nVisualization process complete!")

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Fatal error: {e}")
        import traceback
        traceback.print_exc()